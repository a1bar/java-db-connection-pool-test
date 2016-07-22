package pl.edu.pja.master.s12621;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by bartosz.drabik
 */
public class App {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("application/applicationContext.xml");
    }
}
