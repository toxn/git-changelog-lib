package se.bjurr.gitchangelog.internal.settings;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_DATEFORMAT;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_GITHUB_ISSUE_PATTERN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_GITLAB_ISSUE_PATTERN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_IGNORE_COMMITS_REGEXP;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_JIRA_ISSUE_PATTEN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_MINOR_PATTERN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_NO_ISSUE_NAME;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_PATCH_PATTERN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_READABLE_TAG_NAME;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_REDMINE_ISSUE_PATTEN;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_REMOVE_ISSUE;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_TIMEZONE;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.DEFAULT_UNTAGGED_NAME;
import static se.bjurr.gitchangelog.api.GitChangelogApiConstants.ZERO_COMMIT;

import com.google.gson.Gson;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import se.bjurr.gitchangelog.api.InclusivenessStrategy;
import se.bjurr.gitchangelog.api.model.Changelog;
import se.bjurr.gitchangelog.api.model.Issue;

public class Settings implements Serializable {
  private static final long serialVersionUID = 4565886594381385244L;

  private static Gson gson = new Gson();

  /** Folder where repo lives. */
  private String fromRepo;

  /**
   * Include all commits from here. Any tag or branch name or commit hash. There is a constant
   * pointing at the first commit here: reference{GitChangelogApiConstants#ZERO_COMMIT}.
   */
  private String fromRevision;

  private InclusivenessStrategy fromRevisionStrategy;

  /**
   * Include all commits to this revision. Any tag or branch name or commit hash. There is a
   * constant for master here: reference{GitChangelogApiConstants#REF_MASTER}.
   */
  private String toRevision;

  private InclusivenessStrategy toRevisionStrategy;

  /**
   * A regular expression that is evaluated on each tag. If it matches, the tag will be filtered out
   * and not included in the changelog.
   */
  private String ignoreTagsIfNameMatches;

  /**
   * A regular expression that is evaluated on the commit message of each commit. If it matches, the
   * commit will be filtered out and not included in the changelog.<br>
   * <br>
   * To ignore tags creted by Maven and Gradle release plugins, perhaps you want this: <br>
   * <code>
   * ^\[maven-release-plugin\].*|^\[Gradle Release Plugin\].*|^Merge.*
   * </code><br>
   * <br>
   * Remember to escape it, if added to the json-file it would look like this:<br>
   * <code>
   * ^\\[maven-release-plugin\\].*|^\\[Gradle Release Plugin\\].*|^Merge.*
   * </code>
   */
  private String ignoreCommitsIfMessageMatches;

  /**
   * A date that is evaluated on the commit time of each commit. If the commit is older than the
   * point in time given, then it will be filtered out and not included in the changelog. <br>
   * See {@link SimpleDateFormat}.
   */
  private Date ignoreCommitsIfOlderThan;

  /**
   * Some commits may not be included in any tag. Commits that not released yet may not be tagged.
   * This is a "virtual tag", added to {@link Changelog#getTags()}, that includes those commits. A
   * fitting value may be "Next release".
   */
  private String untaggedName;

  /**
   * Path of template-file to use. It is a Mustache (https://mustache.github.io/) template. Supplied
   * with the context of {@link Changelog}.
   */
  private String templatePath;

  /**
   * Path to the base directory for template partial files. If not null, handlebars will be
   * configured with a FileTemplateLoader with this as base directory.
   */
  private String templateBaseDir;

  /** The filename suffix of template partial files. Requires "templateBaseDir" to be set. */
  private String templateSuffix;

  /**
   * Your tags may look something like <code>git-changelog-maven-plugin-1.6</code>. But in the
   * changelog you just want <code>1.6</code>. With this regular expression, the numbering can be
   * extracted from the tag name.<br>
   * <code>/([^-]+?)$</code>
   */
  private String readableTagName;

  /** Format of dates, see {@link SimpleDateFormat}. */
  private String dateFormat;

  /**
   * This is a "virtual issue" that is added to {@link Changelog#getIssues()}. It contains all
   * commits that has no issue in the commit comment. This could be used as a "wall of shame"
   * listing commiters that did not tag there commits with an issue.
   */
  private String noIssueName;

  /**
   * When date of commits are translated to a string, this timezone is used.<br>
   * <code>UTC</code>
   */
  private String timeZone;

  /**
   * If true, the changelog will not contain the issue in the commit comment. If your changelog is
   * grouped by issues, you may want this to be true. If not grouped by issue, perhaps false.
   */
  private boolean removeIssueFromMessage;

  /** Use any configured feature with Jira. */
  private boolean jiraEnabled;

  /**
   * URL pointing at your JIRA server. When configured, the {@link Issue#getTitle()} will be
   * populated with title from JIRA.<br>
   * <code>https://jiraserver/jira</code>
   */
  private String jiraServer;

  /**
   * Pattern to recognize JIRA:s. <code>\b[a-zA-Z]([a-zA-Z]+)-([0-9]+)\b</code><br>
   * <br>
   * Or escaped if added to json-file:<br>
   * <code>\\b[a-zA-Z]([a-zA-Z]+)-([0-9]+)\\b</code>
   */
  private String jiraIssuePattern;

  /** Additional fields to load for the issues. */
  private List<String> jiraIssueAdditionalFields;

  /** Authenticate to JIRA. */
  private String jiraUsername;

  /** Authenticate to JIRA. */
  private String jiraPassword;

  /** Authenticate to JIRA. */
  private String jiraToken;

  /** Authenticate to JIRA. */
  private String jiraBearer;

  /** Use any configured feature with Redmine. */
  private boolean redmineEnabled;

  /**
   * URL pointing at your Redmine server. When configured, the {@link Issue#getTitle()} will be
   * populated with title from Redmine.<br>
   * <code>https://redmine/redmine</code>
   */
  private String redmineServer;

  /** Pattern to recognize Redmine:s. <code>#([0-9]+)</code> */
  private String redmineIssuePattern;

  /** Authenticate to Redmine. */
  private String redmineUsername;

  /** Authenticate to Redmine. */
  private String redminePassword;

  /** Authenticate to Redmine whith API_KEY */
  private String redmineToken;

  /** Use any configured feature with Github. */
  private boolean gitHubEnabled;

  /**
   * URL pointing at GitHub API. When configured, the {@link Issue#getTitle()} will be populated
   * with title from GitHub.<br>
   * <code>https://api.github.com/repos/tomasbjerre/git-changelog-lib</code>
   */
  private String gitHubApi;

  /**
   * GitHub authentication token. Configure to avoid low rate limits imposed by GitHub in case you
   * have a lot of issues and/or pull requests.<br>
   * <code>https://api.github.com/repos/tomasbjerre/git-changelog-lib</code>
   */
  private String gitHubToken;

  /** Pattern to recognize GitHub:s. <code>#([0-9]+)</code> */
  private String gitHubIssuePattern;

  /**
   * Custom issues are added to support any kind of issue management, perhaps something that is
   * internal to your project. See {@link SettingsIssue}.
   */
  private List<SettingsIssue> customIssues;

  /**
   * Extended variables is simply a key-value mapping of variables that are made available in the
   * template. Is used, for example, by the Bitbucket plugin to supply some internal variables to
   * the changelog context.
   */
  private Map<String, Object> extendedVariables = new HashMap<>();

  /**
   * Extended headers is simply a key-value mapping of headers that will be passed to REST request.
   * Is used, for example, to bypass 2-factor authentication.
   */
  private Map<String, String> extendedRestHeaders;

  /** Commits that don't have any issue in their commit message will not be included. */
  private boolean ignoreCommitsWithoutIssue;

  /** Use any configured feature with Gitlab. */
  private boolean gitLabEnabled;

  /** GitLab server URL, like https://gitlab.com/. */
  private String gitLabServer;

  private String gitLabToken;

  /** Pattern to recognize GitLab:s. <code>#([0-9]+)</code> */
  private String gitLabIssuePattern;

  /**
   * Like: tomas.bjerre85/violations-test for this repo:
   * https://gitlab.com/tomas.bjerre85/violations-test
   */
  private String gitLabProjectName;

  /** Regular expression to use when determining next semantic version based on commits. */
  private String semanticMajorPattern;

  /** Regular expression to use when determining next semantic version based on commits. */
  private String semanticMinorPattern;

  /** Regular expression to use when determining next semantic version based on commits. */
  private String semanticPatchPattern;

  /** Integrate with services to get more details about issues. */
  private boolean useIntegrations;

  /** Path filters to use for filtering commits */
  private List<String> pathFilters;

  private String encoding = StandardCharsets.UTF_8.name();

  public Settings() {}

  public void setCustomIssues(final List<SettingsIssue> customIssues) {
    this.customIssues = customIssues;
  }

  public void setFromRevision(final String revision) {
    if (revision == null || revision.trim().isEmpty()) {
      this.fromRevision = null;
      return;
    }
    this.fromRevision = revision.trim();
  }

  public void setFromRevisionStrategy(final InclusivenessStrategy fromRevisionStrategy) {
    this.fromRevisionStrategy = fromRevisionStrategy;
  }

  public void setToRevision(final String revision) {
    if (revision == null || revision.trim().isEmpty()) {
      this.toRevision = null;
      return;
    }
    this.toRevision = revision.trim();
  }

  public void setToRevisionStrategy(final InclusivenessStrategy toRevisionStrategy) {
    this.toRevisionStrategy = toRevisionStrategy;
  }

  public Optional<String> getFromRevision() {
    return ofNullable(this.fromRevision);
  }

  public InclusivenessStrategy getFromRevisionStrategy() {
    return ofNullable(this.fromRevisionStrategy).orElse(InclusivenessStrategy.DEFAULT);
  }

  public Optional<String> getToRevision() {
    return ofNullable(this.toRevision);
  }

  public InclusivenessStrategy getToRevisionStrategy() {
    return ofNullable(this.toRevisionStrategy).orElse(InclusivenessStrategy.DEFAULT);
  }

  public void setFromRepo(final String fromRepo) {
    this.fromRepo = fromRepo;
  }

  public String getFromRepo() {
    return ofNullable(this.fromRepo).orElse(".");
  }

  public void setIgnoreCommitsIfMessageMatches(final String ignoreCommitsIfMessageMatches) {
    this.ignoreCommitsIfMessageMatches = ignoreCommitsIfMessageMatches;
  }

  public void setIgnoreTagsIfNameMatches(final String ignoreTagsIfNameMatches) {
    this.ignoreTagsIfNameMatches = ignoreTagsIfNameMatches;
  }

  public void setIgnoreCommitsIfOlderThan(final Date ignoreCommitsIfOlderThan) {
    if (ignoreCommitsIfOlderThan != null) {
      this.ignoreCommitsIfOlderThan = new Date(ignoreCommitsIfOlderThan.getTime());
    } else {
      this.ignoreCommitsIfOlderThan = null;
    }
  }

  public void setJiraIssuePattern(final String jiraIssuePattern) {
    this.jiraIssuePattern = jiraIssuePattern;
  }

  public void setJiraServer(final String jiraServer) {
    this.jiraServer = jiraServer;
  }

  public void setRedmineIssuePattern(final String redmineIssuePattern) {
    this.redmineIssuePattern = redmineIssuePattern;
  }

  public void setRedmineServer(final String redmineServer) {
    this.redmineServer = redmineServer;
  }

  public void addCustomIssue(final SettingsIssue customIssue) {
    if (this.customIssues == null) {
      this.customIssues = new ArrayList<>();
    }
    this.customIssues.add(customIssue);
  }

  public List<SettingsIssue> getCustomIssues() {
    if (this.customIssues == null) {
      return new ArrayList<>();
    } else {
      return this.customIssues;
    }
  }

  public String getIgnoreCommitsIfMessageMatches() {
    return ofNullable(this.ignoreCommitsIfMessageMatches).orElse(DEFAULT_IGNORE_COMMITS_REGEXP);
  }

  public Optional<Date> getIgnoreCommitsIfOlderThan() {
    return ofNullable(this.ignoreCommitsIfOlderThan);
  }

  public String getJiraIssuePattern() {
    return ofNullable(this.jiraIssuePattern).orElse(DEFAULT_JIRA_ISSUE_PATTEN);
  }

  public Optional<String> getJiraServer() {
    return ofNullable(this.jiraServer);
  }

  public String getRedmineIssuePattern() {
    return ofNullable(this.redmineIssuePattern).orElse(DEFAULT_REDMINE_ISSUE_PATTEN);
  }

  public Optional<String> getRedmineServer() {
    return ofNullable(this.redmineServer);
  }

  public static Settings fromFile(final URL url) {
    try {
      final String json = new String(Files.readAllBytes(Paths.get(url.toURI())), UTF_8);
      return fromJson(json);
    } catch (final Exception e) {
      throw new RuntimeException("Cannot read " + url, e);
    }
  }

  public static Settings fromJson(final String json) {
    return gson.fromJson(json, Settings.class);
  }

  public String toJson() {
    return gson.toJson(this);
  }

  public Settings copy() {
    return Settings.fromJson(this.toJson());
  }

  public String getUntaggedName() {
    return ofNullable(this.untaggedName).orElse(DEFAULT_UNTAGGED_NAME);
  }

  public Optional<String> getIgnoreTagsIfNameMatches() {
    return ofNullable(this.ignoreTagsIfNameMatches);
  }

  public void setUntaggedName(final String untaggedName) {
    this.untaggedName = untaggedName;
  }

  public String getTemplatePath() {
    return ofNullable(this.templatePath).orElse("changelog.mustache");
  }

  public void setTemplatePath(final String templatePath) {
    this.templatePath = templatePath;
  }

  public String getTemplateBaseDir() {
    return this.templateBaseDir;
  }

  public void setTemplateBaseDir(final String templateBaseDir) {
    this.templateBaseDir = templateBaseDir;
  }

  public String getTemplateSuffix() {
    return this.templateSuffix;
  }

  public void setTemplateSuffix(final String templateSuffix) {
    this.templateSuffix = templateSuffix;
  }

  public String getReadableTagName() {
    return ofNullable(this.readableTagName).orElse(DEFAULT_READABLE_TAG_NAME);
  }

  public String getDateFormat() {
    return ofNullable(this.dateFormat).orElse(DEFAULT_DATEFORMAT);
  }

  public void setDateFormat(final String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public void setNoIssueName(final String noIssueName) {
    this.noIssueName = noIssueName;
  }

  public void setReadableTagName(final String readableTagName) {
    this.readableTagName = readableTagName;
  }

  public String getNoIssueName() {
    return ofNullable(this.noIssueName).orElse(DEFAULT_NO_ISSUE_NAME);
  }

  public void setTimeZone(final String timeZone) {
    this.timeZone = timeZone;
  }

  public String getTimeZone() {
    return ofNullable(this.timeZone).orElse(DEFAULT_TIMEZONE);
  }

  public static Settings defaultSettings() {
    final Settings s = new Settings();
    s.setFromRepo(".");
    s.setFromRevision(ZERO_COMMIT);
    s.setFromRevisionStrategy(InclusivenessStrategy.DEFAULT);
    s.setToRevision("refs/heads/master");
    s.setToRevisionStrategy(InclusivenessStrategy.DEFAULT);
    s.setIgnoreCommitsIfMessageMatches("^Merge.*");
    s.setTemplateSuffix(".hbs");
    s.setReadableTagName("/([^/]+?)$");
    s.setDateFormat("YYYY-MM-dd HH:mm:ss");
    s.setUntaggedName("No tag");
    s.setNoIssueName("No issue");
    s.setTimeZone("UTC");
    s.setRemoveIssueFromMessage(true);
    s.setJiraIssuePattern("\\\\b[a-zA-Z]([a-zA-Z]+)-([0-9]+)\\\\b");
    return s;
  }

  public void setRemoveIssueFromMessage(final boolean removeIssueFromMessage) {
    this.removeIssueFromMessage = removeIssueFromMessage;
  }

  public Boolean removeIssueFromMessage() {
    return ofNullable(this.removeIssueFromMessage).orElse(DEFAULT_REMOVE_ISSUE);
  }

  public Optional<String> getGitHubApi() {
    return ofNullable(this.gitHubApi);
  }

  public Optional<String> getGitHubToken() {
    return ofNullable(this.gitHubToken);
  }

  public void setGitHubApi(final String gitHubApi) {
    this.gitHubApi = gitHubApi;
  }

  public void setGitHubToken(final String gitHubToken) {
    this.gitHubToken = gitHubToken;
  }

  public void setGitHubIssuePattern(final String gitHubIssuePattern) {
    this.gitHubIssuePattern = gitHubIssuePattern;
  }

  public String getGitHubIssuePattern() {
    return ofNullable(this.gitHubIssuePattern).orElse(DEFAULT_GITHUB_ISSUE_PATTERN);
  }

  public Optional<String> getJiraUsername() {
    return ofNullable(this.jiraUsername);
  }

  public void setJiraPassword(final String jiraPassword) {
    this.jiraPassword = jiraPassword;
  }

  public void setJiraToken(final String jiraToken) {
    this.jiraToken = jiraToken;
  }

  public void setJiraBearer(final String jiraBearer) {
    this.jiraBearer = jiraBearer;
  }

  public void setJiraUsername(final String jiraUsername) {
    this.jiraUsername = jiraUsername;
  }

  public Optional<String> getJiraPassword() {
    return ofNullable(this.jiraPassword);
  }

  public Optional<String> getJiraToken() {
    return ofNullable(this.jiraToken);
  }

  public Optional<String> getJiraBearer() {
    return ofNullable(this.jiraBearer);
  }

  public Optional<String> getRedmineUsername() {
    return ofNullable(this.redmineUsername);
  }

  public void setRedminePassword(final String redminePassword) {
    this.redminePassword = redminePassword;
  }

  public void setRedmineToken(final String redmineToken) {
    this.redmineToken = redmineToken;
  }

  public void setRedmineUsername(final String redmineUsername) {
    this.redmineUsername = redmineUsername;
  }

  public Optional<String> getRedminePassword() {
    return ofNullable(this.redminePassword);
  }

  public Optional<String> getRedmineToken() {
    return ofNullable(this.redmineToken);
  }

  public void setExtendedVariables(final Map<String, Object> extendedVariables) {
    this.extendedVariables = extendedVariables;
  }

  public Map<String, Object> getExtendedVariables() {
    return this.extendedVariables;
  }

  public Map<String, String> getExtendedRestHeaders() {
    return this.extendedRestHeaders;
  }

  public void setExtendedRestHeaders(final Map<String, String> extendedRestHeaders) {
    this.extendedRestHeaders = extendedRestHeaders;
  }

  public void setIgnoreCommitsWithoutIssue(final boolean ignoreCommitsWithoutIssue) {
    this.ignoreCommitsWithoutIssue = ignoreCommitsWithoutIssue;
  }

  public boolean ignoreCommitsWithoutIssue() {
    return this.ignoreCommitsWithoutIssue;
  }

  public void setGitLabIssuePattern(final String gitLabIssuePattern) {
    this.gitLabIssuePattern = gitLabIssuePattern;
  }

  public void setGitLabProjectName(final String gitLabProjectName) {
    this.gitLabProjectName = gitLabProjectName;
  }

  public void setGitLabServer(final String gitLabServer) {
    this.gitLabServer = gitLabServer;
  }

  public void setGitLabToken(final String gitLabToken) {
    this.gitLabToken = gitLabToken;
  }

  public Optional<String> getGitLabServer() {
    return ofNullable(this.gitLabServer);
  }

  public Optional<String> getGitLabToken() {
    return ofNullable(this.gitLabToken);
  }

  public String getGitLabIssuePattern() {
    return ofNullable(this.gitLabIssuePattern).orElse(DEFAULT_GITLAB_ISSUE_PATTERN);
  }

  public Optional<String> getGitLabProjectName() {
    return ofNullable(this.gitLabProjectName);
  }

  public Optional<String> getSemanticMajorPattern() {
    return ofNullable(this.semanticMajorPattern);
  }

  public void setSemanticMajorPattern(final String semanticMajorPattern) {
    this.semanticMajorPattern = this.isRegexp(semanticMajorPattern, "semanticMajorPattern");
  }

  public String getSemanticMinorPattern() {
    return ofNullable(this.semanticMinorPattern).orElse(DEFAULT_MINOR_PATTERN);
  }

  public void setSemanticMinorPattern(final String semanticMinorPattern) {
    this.semanticMinorPattern = this.isRegexp(semanticMinorPattern, "semanticMinorPattern");
  }

  public String getSemanticPatchPattern() {
    return ofNullable(this.semanticPatchPattern).orElse(DEFAULT_PATCH_PATTERN);
  }

  public void setSemanticPatchPattern(final String semanticPatchPattern) {
    this.semanticPatchPattern = this.isRegexp(semanticPatchPattern, "semanticPatchPattern");
  }

  private String isRegexp(final String pattern, final String string) {
    try {
      Pattern.compile(pattern);
    } catch (final PatternSyntaxException e) {
      throw new RuntimeException(pattern + " in " + string + " is not valid regexp.", e);
    }
    return pattern;
  }

  public void setGitHubEnabled(final boolean githubEnabled) {
    this.gitHubEnabled = githubEnabled;
  }

  public void setGitLabEnabled(final boolean gitlabEnabled) {
    this.gitLabEnabled = gitlabEnabled;
  }

  public void setJiraEnabled(final boolean jiraEnabled) {
    this.jiraEnabled = jiraEnabled;
  }

  public void setRedmineEnabled(final boolean redmineEnabled) {
    this.redmineEnabled = redmineEnabled;
  }

  public boolean isGitHubEnabled() {
    return this.gitHubEnabled;
  }

  public boolean isGitLabEnabled() {
    return this.gitLabEnabled;
  }

  public boolean isJiraEnabled() {
    return this.jiraEnabled;
  }

  public boolean isRedmineEnabled() {
    return this.redmineEnabled;
  }

  public void setUseIntegrations(final boolean useIntegrations) {
    this.useIntegrations = useIntegrations;
  }

  public boolean isUseIntegrations() {
    return this.useIntegrations;
  }

  public void setEncoding(final Charset encoding) {
    this.encoding = encoding.name();
  }

  public Charset getEncoding() {
    return Charset.forName(this.encoding);
  }

  public void setPathFilters(final List<String> pathFilters) {
    this.pathFilters = pathFilters;
  }

  public List<String> getPathFilters() {
    return ofNullable(this.pathFilters).orElse(new ArrayList<>());
  }

  public List<String> getJiraIssueAdditionalFields() {
    if (this.jiraIssueAdditionalFields == null) {
      return new ArrayList<>();
    } else {
      return this.jiraIssueAdditionalFields;
    }
  }

  public void setJiraIssueAdditionalFields(final List<String> jiraIssueAdditionalFields) {
    this.jiraIssueAdditionalFields = jiraIssueAdditionalFields;
  }

  public void addJiraIssueAdditionalField(final String jiraIssueAdditionalField) {
    if (this.jiraIssueAdditionalFields == null) {
      this.jiraIssueAdditionalFields = new ArrayList<>();
    }
    this.jiraIssueAdditionalFields.add(jiraIssueAdditionalField);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.customIssues,
        this.dateFormat,
        this.encoding,
        this.extendedRestHeaders,
        this.extendedVariables,
        this.fromRepo,
        this.fromRevision,
        this.fromRevisionStrategy,
        this.gitHubApi,
        this.gitHubEnabled,
        this.gitHubIssuePattern,
        this.gitHubToken,
        this.gitLabEnabled,
        this.gitLabIssuePattern,
        this.gitLabProjectName,
        this.gitLabServer,
        this.gitLabToken,
        this.ignoreCommitsIfMessageMatches,
        this.ignoreCommitsIfOlderThan,
        this.ignoreCommitsWithoutIssue,
        this.ignoreTagsIfNameMatches,
        this.jiraBearer,
        this.jiraEnabled,
        this.jiraIssueAdditionalFields,
        this.jiraIssuePattern,
        this.jiraPassword,
        this.jiraServer,
        this.jiraToken,
        this.jiraUsername,
        this.noIssueName,
        this.readableTagName,
        this.redmineEnabled,
        this.redmineIssuePattern,
        this.redminePassword,
        this.redmineServer,
        this.redmineToken,
        this.redmineUsername,
        this.removeIssueFromMessage,
        this.semanticMajorPattern,
        this.semanticMinorPattern,
        this.semanticPatchPattern,
        this.pathFilters,
        this.templateBaseDir,
        this.templatePath,
        this.templateSuffix,
        this.timeZone,
        this.toRevision,
        this.toRevisionStrategy,
        this.untaggedName,
        this.useIntegrations);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final Settings other = (Settings) obj;
    return Objects.equals(this.customIssues, other.customIssues)
        && Objects.equals(this.dateFormat, other.dateFormat)
        && Objects.equals(this.encoding, other.encoding)
        && Objects.equals(this.extendedRestHeaders, other.extendedRestHeaders)
        && Objects.equals(this.extendedVariables, other.extendedVariables)
        && Objects.equals(this.fromRepo, other.fromRepo)
        && Objects.equals(this.fromRevision, other.fromRevision)
        && this.fromRevisionStrategy == other.fromRevisionStrategy
        && Objects.equals(this.gitHubApi, other.gitHubApi)
        && this.gitHubEnabled == other.gitHubEnabled
        && Objects.equals(this.gitHubIssuePattern, other.gitHubIssuePattern)
        && Objects.equals(this.gitHubToken, other.gitHubToken)
        && this.gitLabEnabled == other.gitLabEnabled
        && Objects.equals(this.gitLabIssuePattern, other.gitLabIssuePattern)
        && Objects.equals(this.gitLabProjectName, other.gitLabProjectName)
        && Objects.equals(this.gitLabServer, other.gitLabServer)
        && Objects.equals(this.gitLabToken, other.gitLabToken)
        && Objects.equals(this.ignoreCommitsIfMessageMatches, other.ignoreCommitsIfMessageMatches)
        && Objects.equals(this.ignoreCommitsIfOlderThan, other.ignoreCommitsIfOlderThan)
        && this.ignoreCommitsWithoutIssue == other.ignoreCommitsWithoutIssue
        && Objects.equals(this.ignoreTagsIfNameMatches, other.ignoreTagsIfNameMatches)
        && Objects.equals(this.jiraBearer, other.jiraBearer)
        && this.jiraEnabled == other.jiraEnabled
        && Objects.equals(this.jiraIssueAdditionalFields, other.jiraIssueAdditionalFields)
        && Objects.equals(this.jiraIssuePattern, other.jiraIssuePattern)
        && Objects.equals(this.jiraPassword, other.jiraPassword)
        && Objects.equals(this.jiraServer, other.jiraServer)
        && Objects.equals(this.jiraToken, other.jiraToken)
        && Objects.equals(this.jiraUsername, other.jiraUsername)
        && Objects.equals(this.noIssueName, other.noIssueName)
        && Objects.equals(this.readableTagName, other.readableTagName)
        && this.redmineEnabled == other.redmineEnabled
        && Objects.equals(this.redmineIssuePattern, other.redmineIssuePattern)
        && Objects.equals(this.redminePassword, other.redminePassword)
        && Objects.equals(this.redmineServer, other.redmineServer)
        && Objects.equals(this.redmineToken, other.redmineToken)
        && Objects.equals(this.redmineUsername, other.redmineUsername)
        && this.removeIssueFromMessage == other.removeIssueFromMessage
        && Objects.equals(this.semanticMajorPattern, other.semanticMajorPattern)
        && Objects.equals(this.semanticMinorPattern, other.semanticMinorPattern)
        && Objects.equals(this.semanticPatchPattern, other.semanticPatchPattern)
        && Objects.equals(this.pathFilters, other.pathFilters)
        && Objects.equals(this.templateBaseDir, other.templateBaseDir)
        && Objects.equals(this.templatePath, other.templatePath)
        && Objects.equals(this.templateSuffix, other.templateSuffix)
        && Objects.equals(this.timeZone, other.timeZone)
        && Objects.equals(this.toRevision, other.toRevision)
        && this.toRevisionStrategy == other.toRevisionStrategy
        && Objects.equals(this.untaggedName, other.untaggedName)
        && this.useIntegrations == other.useIntegrations;
  }

  @Override
  public String toString() {
    return "Settings [fromRepo="
        + this.fromRepo
        + ", fromRevision="
        + this.fromRevision
        + ", fromRevisionStrategy="
        + this.fromRevisionStrategy
        + ", toRevision="
        + this.toRevision
        + ", toRevisionStrategy="
        + this.toRevisionStrategy
        + ", ignoreTagsIfNameMatches="
        + this.ignoreTagsIfNameMatches
        + ", ignoreCommitsIfMessageMatches="
        + this.ignoreCommitsIfMessageMatches
        + ", ignoreCommitsIfOlderThan="
        + this.ignoreCommitsIfOlderThan
        + ", untaggedName="
        + this.untaggedName
        + ", templatePath="
        + this.templatePath
        + ", templateBaseDir="
        + this.templateBaseDir
        + ", templateSuffix="
        + this.templateSuffix
        + ", readableTagName="
        + this.readableTagName
        + ", dateFormat="
        + this.dateFormat
        + ", noIssueName="
        + this.noIssueName
        + ", timeZone="
        + this.timeZone
        + ", removeIssueFromMessage="
        + this.removeIssueFromMessage
        + ", jiraEnabled="
        + this.jiraEnabled
        + ", jiraServer="
        + this.jiraServer
        + ", jiraIssuePattern="
        + this.jiraIssuePattern
        + ", jiraIssueAdditionalFields="
        + this.jiraIssueAdditionalFields
        + ", jiraUsername="
        + this.jiraUsername
        + ", jiraPassword="
        + this.jiraPassword
        + ", jiraToken="
        + this.jiraToken
        + ", jiraBearer="
        + this.jiraBearer
        + ", redmineEnabled="
        + this.redmineEnabled
        + ", redmineServer="
        + this.redmineServer
        + ", redmineIssuePattern="
        + this.redmineIssuePattern
        + ", redmineUsername="
        + this.redmineUsername
        + ", redminePassword="
        + this.redminePassword
        + ", redmineToken="
        + this.redmineToken
        + ", gitHubEnabled="
        + this.gitHubEnabled
        + ", gitHubApi="
        + this.gitHubApi
        + ", gitHubToken="
        + this.gitHubToken
        + ", gitHubIssuePattern="
        + this.gitHubIssuePattern
        + ", customIssues="
        + this.customIssues
        + ", extendedVariables="
        + this.extendedVariables
        + ", extendedRestHeaders="
        + this.extendedRestHeaders
        + ", ignoreCommitsWithoutIssue="
        + this.ignoreCommitsWithoutIssue
        + ", gitLabEnabled="
        + this.gitLabEnabled
        + ", gitLabServer="
        + this.gitLabServer
        + ", gitLabToken="
        + this.gitLabToken
        + ", gitLabIssuePattern="
        + this.gitLabIssuePattern
        + ", gitLabProjectName="
        + this.gitLabProjectName
        + ", semanticMajorPattern="
        + this.semanticMajorPattern
        + ", semanticMinorPattern="
        + this.semanticMinorPattern
        + ", semanticPatchPattern="
        + this.semanticPatchPattern
        + ", useIntegrations="
        + this.useIntegrations
        + ", pathFilters="
        + this.pathFilters
        + ", encoding="
        + this.encoding
        + "]";
  }
}
