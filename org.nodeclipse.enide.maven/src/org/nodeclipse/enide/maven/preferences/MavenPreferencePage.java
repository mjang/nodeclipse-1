package org.nodeclipse.enide.maven.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nodeclipse.enide.maven.Activator;

/**
 * @author Paul Verest
 */
public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private DirectoryFieldEditor mavenHome;
    private FileFieldEditor mavenPath;
    private StringFieldEditor mavenOptions;
    
    private BooleanFieldEditor mavenOptionOffline;
    private BooleanFieldEditor mavenOptionDebug;

    public MavenPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(
//        	VersionUtil.getLongString()+  //TODO make plugin to collect Eclipse utils
//        	"\n"+
    		"Maven requires to set JAVA_HOME and MAVEN_HOME environment variable to use from shell\n"
    		+"MAVEN_HOME is folder where you extracted downloaded Maven distribution,"
    		+" e.g. D:\\Progs\\Maven\\apache-maven-3.1.1\n" //D:\Progs\Maven\apache-maven-3.1.1
    		+"Then it is easy to add mvn to Path like \"%JAVA_HOME%\\bin;%MAVEN_HOME%\\bin;\" string"
        		);
    }
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {

		//TODO show maven version
		mavenHome = new DirectoryFieldEditor(MavenConstants.MAVEN_HOME, "Maven home dirrectory:", getFieldEditorParent());
		mavenHome.setEnabled(false, getFieldEditorParent());
		addField(mavenHome);

		mavenPath = new FileFieldEditor(MavenConstants.MAVEN_PATH, "Maven path:", getFieldEditorParent());
		addField(mavenPath);
		mavenOptions = new StringFieldEditor(MavenConstants.MAVEN_OPTIONS, "Maven options (mvn -h):", getFieldEditorParent());
		addField(mavenOptions);

		mavenOptionOffline = new BooleanFieldEditor(MavenConstants.MAVEN_OPTION_OFFLINE, "-o Offline", getFieldEditorParent());
        addField(mavenOptionOffline);
        mavenOptionDebug = new BooleanFieldEditor(MavenConstants.MAVEN_OPTION_DEBUG,"-X Debug", getFieldEditorParent());
        addField(mavenOptionDebug);
	}


}
