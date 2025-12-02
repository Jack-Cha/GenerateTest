package com.testgen;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class TestRunner {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java TestRunner <fully.qualified.TestClassName>");
            System.exit(1);
        }
        
        String testClassName = args[0];
        System.out.println("Running tests for: " + testClassName);
        
        try {
            Class<?> testClass = Class.forName(testClassName);
            System.out.println("Successfully loaded class: " + testClass.getName());
            System.out.println("ClassLoader: " + testClass.getClassLoader());
            
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();
            
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            
            TestExecutionSummary summary = listener.getSummary();
            if (summary != null) {
                System.out.println("\n=== Test Execution Summary ===");
                System.out.println("Tests found: " + summary.getTestsFoundCount());
                System.out.println("Tests started: " + summary.getTestsStartedCount());
                System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
                System.out.println("Tests failed: " + summary.getTestsFailedCount());
                System.out.println("Tests skipped: " + summary.getTestsSkippedCount());
                
                if (summary.getFailures().size() > 0) {
                    System.out.println("\n=== Failures ===");
                    summary.getFailures().forEach(failure -> {
                        System.out.println(failure.getTestIdentifier().getDisplayName());
                        System.out.println(failure.getException());
                    });
                }
            } else {
                System.err.println("ERROR: Test execution summary is null!");
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: Class not found: " + testClassName);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
