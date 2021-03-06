package jenkins.plugins.cucumberJenkins;

import hudson.Extension;
import hudson.model.*;
import hudson.tasks.junit.*;
import hudson.Util;
import hudson.views.ListViewColumn;
import hudson.model.Descriptor.FormException;
import hudson.util.CaseInsensitiveComparator;
import hudson.util.FormFieldValidator;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import javax.servlet.ServletException;
import java.math.BigDecimal;
import java.io.IOException;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.*;
import org.apache.commons.collections.CollectionUtils;

public class CucumberJenkinsView extends ListView {
    private final int millisecondsInAMinute = 60000;
    private final double minutesInAnHour = 60.0;

    @DataBoundConstructor
    public CucumberJenkinsView(String name) {
        super(name);
    }

  @Extension
  public static final class CucumberJenkinsViewDescriptor extends ViewDescriptor {

    @Override
    public String getDisplayName() {
      return "Cucumber Jenkins Monitor by Fabrice";
    }

  }

    public String getProjectName() { return projectName; }
    public String getRefresh() { return refresh; }

    public String getFirstRegexp(){ return firstRegexp; }
    public String getFirstDisplay() { return firstDisplay; }

    public String getSecondRegexp(){ return secondRegexp; }
    public String getSecondDisplay() { return secondDisplay; }

    public String getThirdRegexp(){ return thirdRegexp; }
    public String getThirdDisplay() { return thirdDisplay; }

    public String getTotalRegexp(){ return totalRegexp; }
    public String getTotalDisplay() { return totalDisplay; }
    public String getSystemRegexp(){ return systemRegexp; }

	public String getDebug() { return debug;}

    private String projectName;
    private String refresh;
    private String firstRegexp;
	private String firstDisplay;
    private String secondRegexp;
	private String secondDisplay;
    private String thirdRegexp;
	private String thirdDisplay;
    private String totalRegexp;
	private String totalDisplay;
	private String systemRegexp;
	private String debug;
	public String showAll;


    @Override
    protected void submit(StaplerRequest req) throws ServletException,
          Descriptor.FormException, IOException {
		super.submit(req);
		this.projectName = (req.getParameter("projectName") != null) ? req.getParameter("projectName") : "Control Panel";
		this.refresh = (req.getParameter("refresh") != null) ? req.getParameter("refresh") : "30";

		this.firstRegexp = (req.getParameter("firstRegexp") != null) ? req.getParameter("firstRegexp") : "^FF-.*";
		this.firstDisplay = (req.getParameter("firstDisplay") != null) ? req.getParameter("firstDisplay") : "Firefox";
		this.secondRegexp = (req.getParameter("secondRegexp") != null) ? req.getParameter("secondRegexp") : "^CH-.*";
		this.secondDisplay = (req.getParameter("secondDisplay") != null) ? req.getParameter("secondDisplay") : "Chrome";
		this.thirdRegexp = (req.getParameter("thirdRegexp") != null) ? req.getParameter("thirdRegexp") : "^IE-.*";
		this.thirdDisplay = (req.getParameter("thirdDisplay") != null) ? req.getParameter("thirdDisplay") : "Internet Explorer";
		this.systemRegexp = (req.getParameter("systemRegexp") != null) ? req.getParameter("systemRegexp") : "^_.*";

		this.totalDisplay = (req.getParameter("totalDisplay") != null) ? req.getParameter("totalDisplay") : "Total";
		this.totalRegexp = "("+this.firstRegexp+"|"+this.secondRegexp+"|"+this.thirdRegexp+"|"+this.systemRegexp+")";

		this.showAll = (req.getParameter("showAll") != null) ? req.getParameter("showAll"): "";

		this.debug = "";


	}

    public String getTotalTime() {return getCurrentBuildDuration(totalRegexp); }
    public String getFirstTime() {return getCurrentBuildDuration(firstRegexp); }
    public String getSecondTime() { return getCurrentBuildDuration(secondRegexp); }
    public String getThirdTime() { return getCurrentBuildDuration(thirdRegexp); }

    public String getTotalNbJobs() { return getNbJobs(totalRegexp); }
    public String getFirstNbJobs() { return getNbJobs(firstRegexp); }
    public String getSecondNbJobs() { return getNbJobs(secondRegexp); }
    public String getThirdNbJobs() { return getNbJobs(thirdRegexp); }

    public String getTotalNbJobsFailed() { return getNbJobsFailed(totalRegexp); }
    public String getFirstNbJobsFailed() { return getNbJobsFailed(firstRegexp); }
    public String getSecondNbJobsFailed() { return getNbJobsFailed(secondRegexp); }
    public String getThirdNbJobsFailed() { return getNbJobsFailed(thirdRegexp); }

    public String getFirstJobsFailed() { return getJobsFailed(firstRegexp); }
    public String getSecondJobsFailed() { return getJobsFailed(secondRegexp); }
    public String getThirdJobsFailed() { return getJobsFailed(thirdRegexp); }
	public String getSystemJobs() { return getAllJobs(systemRegexp); }

    private String getCurrentBuildDuration(String pattern) {
		int timeToSave = 0;
		Hudson hudson = Hudson.getInstance();
        List<Job> jobs = hudson.getAllItems(Job.class);

		for(Job job:jobs){
			if (job.getName().toString().matches(pattern)) {
				if (job.getLastBuild() != null) {
					timeToSave += job.getLastBuild().getDuration();
				}
			}
        }
		return convertDurationToDisplay(timeToSave);
    }

    private String getNbJobs(String pattern) {
		int nbJob = 0;
		String logFile = "";
		Hudson hudson = Hudson.getInstance();
        List<Job> jobs = hudson.getAllItems(Job.class);

		for(Job job:jobs){
			if (job.getName().toString().matches(pattern)) {
				if (job.getLastBuild() != null) {
					logFile = job.getLastBuild().getLogFile().toString();
					nbJob += getCucumberLogResult(logFile, "all");
				}
			}
        }
        return String.valueOf(nbJob);
    }

    private String getNbJobsFailed(String pattern) {
		int nbJob = 0;
		String logFile = "";
		Hudson hudson = Hudson.getInstance();
        List<Job> jobs = hudson.getAllItems(Job.class);

		for(Job job:jobs){
			if (job.getName().toString().matches(pattern)) {
				if (job.getLastBuild() != null) {
					if (jobIsFailed(job)) {
						logFile = job.getLastBuild().getLogFile().toString();
						nbJob += getCucumberLogResult(logFile, "failed");
					}
				}
			}
        }

        return String.valueOf(nbJob);
    }

	private Boolean jobIsFailed (Job job) {
		return !(
			job.getLastBuild().getBuildStatusSummary().message.toString().equalsIgnoreCase("stable")
			|| job.getLastBuild().getBuildStatusSummary().message.toString().equalsIgnoreCase("back to normal")
		);
	}

    private String getJobsFailed(String pattern) {
		String failedJobs = "";
		Hudson hudson = Hudson.getInstance();
        List<Job> jobs = hudson.getAllItems(Job.class);

		for(Job job:jobs){
			if (job.getName().toString().matches(pattern)) {
				if (job.getLastBuild() != null) {
					if (job.getLastBuild().isBuilding() || jobIsFailed(job) || this.showAll.equalsIgnoreCase("all")) {
						String logFile = job.getLastBuild().getLogFile().toString();
						int nbScenario = getCucumberLogResult(logFile, "all");
						int nbScenarioFailed = getCucumberLogResult(logFile, "failed");
						String runningMode = " ";
						String runningProgress = "";
						String lastAlert = "";

						if (job.getLastBuild().isBuilding()) {
							int currentDuration = (int)(System.currentTimeMillis() - job.getLastBuild().getTimeInMillis());
							runningMode = "running";
							runningProgress = "<progress class=\"show_run\" value=\""
								+ currentDuration
								+ "\" max=\""
								+ job.getLastBuild().getEstimatedDuration()
								+ "\" />";
						}else if (nbScenario == 0 || nbScenario < nbScenarioFailed) {
							lastAlert = "bigerror";
						}

						failedJobs += "<span><span class=\""
									+ ((jobIsFailed(job))? "error " : "valid ")
									+ runningMode
									+ lastAlert
									+ "\">"+job.getName().toString()
									+ " (" + nbScenarioFailed + " / "+ nbScenario + ")"
									+ "</span>"
									+ runningProgress
									+ "</span>";
					}else{
						this.debug += " - "
							+ this.showAll;
					}
				}
			}
        }

        return failedJobs;
    }

    private String getAllJobs(String pattern) {
		String allJobs = "";
		Hudson hudson = Hudson.getInstance();
        List<Job> jobs = hudson.getAllItems(Job.class);

		for(Job job:jobs){
			if (job.getName().toString().matches(pattern)) {
				if (job.getLastBuild() != null) {
					String runningMode = " ";
					String runningProgress = "";
					if (job.getLastBuild().isBuilding()) {
						int currentDuration = (int)(System.currentTimeMillis() - job.getLastBuild().getTimeInMillis());
						runningMode = "running";
						runningProgress = "<progress class=\"show_run\" value=\""
							+ currentDuration
							+ "\" max=\""
							+ job.getLastBuild().getEstimatedDuration()
							+ "\" />";
					}
					allJobs += "<span><span class=\""
								+ ((jobIsFailed(job))? "error " : "valid ")
								+ ((job.getLastBuild().isBuilding())? "running" : "")
								+ "\">"+job.getName().toString()
								+ "</span>"
								+ runningProgress
								+ "</span>";
				}
			}
        }

        return allJobs;
    }

    private String convertDurationToDisplay(long durationInMillis) {
        long durationInMins = durationInMillis / millisecondsInAMinute;
		int hour = (int) durationInMins/60;
		int min = (int) durationInMins % 60;
		return "<strong>" + String.valueOf(hour) + "</strong> h "
				+ (( min < 10 ) ?  "0" : "") + String.valueOf(min);
    }

    private static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

	private int getCucumberLogResult(String fileName, String which)
	{
		Pattern pattern;
		Pattern failed;
		int result;

		// regexp for cucumber result
		// 18 scenarios (2 failed, 16 passed)
		pattern = Pattern.compile("([0-9]+) scenarios? \\((.*)\\)");
		failed = Pattern.compile("([0-9]+) failed.*");
		result = parseLog(fileName, which, pattern, failed);
		if (result > 0) {
			return result;
		}

		// regexp for Unitest result
		// [exec] Tests: 661, Assertions: 2524, Failures: 1, Skipped: 4.
		pattern = Pattern.compile("\\(([0-9]+) tests");
		failed = Pattern.compile("Failures: ([0-9]+)");
		result = parseLog(fileName, which, pattern, failed);
		if (result > 0) {
			return result;
		}

		// other pattern for unitest result
		// OK (43 tests, 87 assertions)
		pattern = Pattern.compile("Tests: ([0-9]+), Assertions: [0-9]+(.*)");
		result = parseLog(fileName, which, pattern, failed);
		if (result > 0) {
			return result;
		}

		// no result found, send 0
		return 0;
	}

	private static int parseLog (String fileName, String which, Pattern pattern, Pattern failed) {
		int results = 0;
		try{
			// Open the log file
			FileInputStream fstream = new FileInputStream(fileName);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
				Matcher m = pattern.matcher(strLine);
				if (m.find()) {
					if (which == "failed") {
						Matcher f = failed.matcher(m.group(2));
						if (f.find()) {
							results += Integer.parseInt(f.group(1));
						}
					}
					else if (which == "all") {
						results += Integer.parseInt(m.group(1));
					}
				}
			}
			//Close the input stream
			in.close();
		}catch (Exception e){//Catch exception if any
			// no log file, return 0
			return 0;
		}
		return results;
	}
}
