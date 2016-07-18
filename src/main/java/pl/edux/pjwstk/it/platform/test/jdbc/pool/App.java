package pl.edux.pjwstk.it.platform.test.jdbc.pool;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by bartosz.drabik on 7/18/2016.
 */
public class App {
    public static void main(String[] args) {
        final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("application/applicationContext.xml");
        final Simulation simulation = (Simulation) applicationContext.getBean("Simulation");
    }
}
