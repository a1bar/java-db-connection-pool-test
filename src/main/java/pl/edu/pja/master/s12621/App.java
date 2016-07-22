package pl.edu.pja.master.s12621;

import org.springframework.context.support.GenericXmlApplicationContext;
import pl.edu.pja.master.s12621.utils.AppProfiles;

/**
 * Created by bartosz.drabik
 */
public class App {
    public static void main(String[] args) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext();
        String profileName = args.length > 0 ? args[0] : AppProfiles.getDefaultProfile();
        context.getEnvironment().setActiveProfiles(AppProfiles.getAppProfile(profileName));
        context.load("application/applicationContext.xml");
        context.refresh();
    }
}
