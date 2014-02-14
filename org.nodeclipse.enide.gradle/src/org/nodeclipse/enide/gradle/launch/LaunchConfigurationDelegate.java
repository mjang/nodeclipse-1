package org.nodeclipse.enide.gradle.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
//import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jface.preference.IPreferenceStore;
import org.nodeclipse.common.preferences.CommonDialogs;
import org.nodeclipse.enide.gradle.Activator;
//import org.eclipse.jface.preference.IPreferenceStore;
//import org.nodeclipse.debug.util.Constants;
//import org.nodeclipse.debug.util.VariablesUtil;
//import org.nodeclipse.ui.Activator;
//import org.nodeclipse.ui.preferences.Dialogs;
//import org.nodeclipse.ui.preferences.PreferenceConstants;
//import org.nodeclipse.ui.util.NodeclipseConsole;
import org.nodeclipse.enide.gradle.preferences.GradleConstants;
import org.nodeclipse.enide.gradle.util.NodeclipseLogger;
import org.nodeclipse.enide.gradle.util.VariablesUtil;

/**
 * `build.gradle` Run As gradle build<br>
 * see LaunchConfigurationDelegate in .debug and .phantomjs, .jjs, .enide.maven module for comparison.
 * 
 * @since 0.10
 * @author Paul Verest
 */
public class LaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	private boolean warned = false;
	
	//specific
	protected void specialOptions(ILaunchConfiguration configuration,
			IPreferenceStore preferenceStore, List<String> cmdLine) throws CoreException {
		cmdLine.add("build");
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		
		// Using configuration to build command line	
		List<String> cmdLine = new ArrayList<String>();
		
		// Gradle installation path should be stored in preference.
		String gradlePath= preferenceStore.getString(GradleConstants.GRADLE_PATH);
		// Check if the maven location is correctly configured
		File mavenFile = new File(gradlePath);
		if(!mavenFile.exists()){
			// If the location is not valid than show a dialog which prompts the user to goto the preferences page
			CommonDialogs.showPreferencesDialog(GradleConstants.PREFERENCES_PAGE,
					"Gradle installation is not correctly configured.\n\n"
					+ "Please goto Window -> Preferences -> "+GradleConstants.PREFERENCE_PAGE_NAME
					+" and configure the correct location");
			return;
		}			
		cmdLine.add(gradlePath);
		
		//{ TODO should be special as --gui mode is likely doesn't care
		if (preferenceStore.getBoolean(GradleConstants.GRADLE_OPTION_DEBUG))
			cmdLine.add("-d");
		if (preferenceStore.getBoolean(GradleConstants.GRADLE_OPTION_INFO))
			cmdLine.add("-i");
		if (preferenceStore.getBoolean(GradleConstants.GRADLE_OPTION_QUIET))
			cmdLine.add("-q");
		if (preferenceStore.getBoolean(GradleConstants.GRADLE_OPTION_OFFLINE))
			cmdLine.add("--offline");
		

		String nodeOptions= preferenceStore.getString(GradleConstants.GRADLE_OPTIONS);
		if(!nodeOptions.equals("")) {
			String[] sa = nodeOptions.split(" ");
			for(String s : sa) {
				cmdLine.add(s);
			}			
		}
		//{

		String file = configuration.getAttribute(GradleConstants.KEY_FILE_PATH,	"");
		String filePath = ResourcesPlugin.getWorkspace().getRoot().findMember(file).getLocation().toOSString();
		// path is relative, so cannot find it, unless get absolute path
		cmdLine.add("-b");  //  -b, --build-file        Specifies the build file.
		cmdLine.add(filePath);
		
		//cmdLine.add("build");
		specialOptions(configuration, preferenceStore, cmdLine);
		
		
		File workingPath = null;
		String workingDirectory = configuration.getAttribute(GradleConstants.ATTR_WORKING_DIRECTORY, "");
		if(workingDirectory.length() > 0) {
			workingDirectory = VariablesUtil.resolveValue(workingDirectory);
			if(workingDirectory != null) {
				workingPath = new File(workingDirectory);
			}
		}
		if (workingPath == null){
			workingPath = (new File(filePath)).getParentFile();
		}

		//env
		String[] envp = getEnvironmentVariables(configuration); 
		
		StringBuilder sb = new StringBuilder(100);
		for(String s : cmdLine) sb.append(s).append(' ');
		NodeclipseLogger.log(sb.append('\n').toString());

		String[] cmds = {};
		cmds = cmdLine.toArray(cmds);
		// Launch a process to debug.eg,
		Process p = DebugPlugin.exec(cmds, workingPath, envp);
		RuntimeProcess process = (RuntimeProcess)DebugPlugin.newProcess(launch, p, GradleConstants.PROCESS_MESSAGE);
		
	}

	/** Get EnvironmentVariables from ILaunchConfiguration
	 * and adds JAVA_HOME, GRADLE_HOME, PATH, TEMP, SystemDrive, HOME 
	 * @param configuration ILaunchConfiguration
	 * @return String[]
	 * @throws CoreException
	 */
	protected String[] getEnvironmentVariables(ILaunchConfiguration configuration) throws CoreException {
		Map<String, String> envm = new HashMap<String, String>();
		envm = configuration.getAttribute(GradleConstants.ATTR_ENVIRONMENT_VARIABLES, envm);
		String[] envp = new String[envm.size() + 2 + 4 + 2];
		int idx = 0;
		for(String key : envm.keySet()) {
			String value = envm.get(key);
			envp[idx++] = key + "=" + value;
		}
		envp[idx++] = "JAVA_HOME=" + System.getProperty("java.home"); //System.getenv("JAVA_HOME");
		//FAILURE: Build failed with an exception.
		//
		//* What went wrong:
		//java.lang.ExceptionInInitializerError (no error message)
		//
		//* Try:
		//Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.
		envp[idx++] = "GRADLE_HOME=" + System.getenv("GRADLE_HOME"); //TODO must be preferenced HOME
		//+ #81
		envp[idx++] = "PATH=" + System.getenv("PATH");
		envp[idx++] = "TEMP=" + System.getenv("TEMP");
		envp[idx++] = "TMP=" + System.getenv("TMP");
		envp[idx++] = "SystemDrive=" + System.getenv("SystemDrive");
		//+
		envp[idx++] = "HOME=" + System.getenv("HOME");
		envp[idx++] = "USERPROFILE=" + System.getenv("USERPROFILE");
		
		if (!warned ){
			StringBuilder sb = new StringBuilder(100);
			for(int i=0; i<envp.length; i++){
				sb.append(envp[i]).append('\n');	
			}
			NodeclipseLogger.log(sb.toString());
			NodeclipseLogger.log("Warning: JAVA_HOME, GRADLE_HOME and others environment variables will be applied automatically to every `gradle` launch.\n");
			warned = true;
		}
		return envp;
	}
}
