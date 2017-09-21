import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec

public class RMIRegistry extends Exec {

    String m_port
    public void port(String port) {
        this.m_port = port
    }

    @Override
    protected void exec() {
        // Check if running on windows
        if (System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('windows')) {
            throw new GradleException('The program was not tested on Windows.\nPlease start rmiregistry manually: ' +
                    "start rmiregistry ${m_port}")
        } else {
            println "Starting rmiregistry at port ${m_port} ..."
        }
        this.commandLine "rmiregistry", m_port
        super.exec()
    }
}