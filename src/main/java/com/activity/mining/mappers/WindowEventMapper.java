package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.WindowAction;
import cc.kave.commons.model.events.visualstudio.WindowEvent;
import com.activity.mining.Activity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;


/** Ported from Kave.FeedbackProcessor WindowEventToActivityMapper.cs
 */
public class WindowEventMapper implements EventToActivityMapper<WindowEvent> {

    private static final List<String> workItemIndicators = List.of(
            "Backlog Item ",
            "Bug ",
            "Initiative ",
            "Query ",
            "Requirement ",
            "Requirements ",
            "Task ",
            "User Story ",
            ".wiq", // 'work item query' file extension
            "[Editor]", // marker seen only in captions like "my open tasks [Editor]"
            "[Results]" // marker seen only in captions like "my open tasks [Results]"
    );

    //Be carefull, the strings are regexes...
    private static Map<String,Activity> toolWindowMapping = Map.ofEntries(
            Map.entry(".NET Reflector .+", Activity.Development),
            Map.entry("Disassembly", Activity.Development),
            Map.entry("Diagram", Activity.Development),
            Map.entry("UML Model Explorer", Activity.Development),
            Map.entry("XML Schema Explorer", Activity.Development),
            Map.entry("Code Analysis", Activity.Development),
            Map.entry("Analysis .+", Activity.Development),
            Map.entry("Analyze .+",Activity.Development),
            Map.entry("Call Hierarchy", Activity.Navigation),
            Map.entry("Hierarchy", Activity.Navigation),
            Map.entry("Inspection Results", Activity.Development),
            Map.entry("Code Coverage Results", Activity.Development),
            Map.entry("Tests Explorer", Activity.Development),
            Map.entry("Test Results",Activity.Development),
            Map.entry("Test Runs",Activity.Development),
            Map.entry("Unit Test Explorer", Activity.Development),
            Map.entry("Unit Test Sessions .+", Activity.Development),
            Map.entry("NCrunch Metrics", Activity.Development),
            Map.entry("NCrunch Tests", Activity.Development),
            Map.entry("Build Explorer", Activity.Development),
            Map.entry("Builds", Activity.Development),
            Map.entry("BuildVision", Activity.Development),
            Map.entry("Folder Difference", Activity.ProjectManagement),
            Map.entry("History", Activity.ProjectManagement),
            Map.entry("Pending Changes.+",Activity.ProjectManagement),
            Map.entry("Resolve Conflicts", Activity.ProjectManagement),
            Map.entry("Source Control Explorer", Activity.ProjectManagement),
            Map.entry("Team Explorer.+", Activity.ProjectManagement),
            Map.entry("Tracking Changeset.+", Activity.ProjectManagement),
            Map.entry("Breakpoints", Activity.Development),
            Map.entry("Call Stack", Activity.Development),
            Map.entry("Locals", Activity.Development),
            Map.entry("Watch.+", Activity.Development),
            Map.entry("Parallel Watch.+", Activity.Development),
            Map.entry("Threads", Activity.Development),
            Map.entry("IntelliTrace", Activity.Development),
            Map.entry("Modules", Activity.Development),
            Map.entry("Immediate Window", Activity.Development),
            Map.entry("Python .+ Interactive", Activity.Development),
            Map.entry("Registers", Activity.Development),
            Map.entry("Error List", Activity.Development),
            Map.entry("Errors in Solution", Activity.Development),
            Map.entry("Find and Replace", Activity.Development),
            Map.entry("Find in Source Control", Activity.Navigation),
            Map.entry("Find Results", Activity.Navigation),
            Map.entry("Find Symbol Results", Activity.Navigation),
            Map.entry("Class View", Activity.Navigation),
            Map.entry("Resource View.+", Activity.Navigation),
            Map.entry("Command Window",Activity.Other),
            Map.entry("Feedback Manager",Activity.Other),
            Map.entry("Notifications", Activity.Other),
            Map.entry("Recommendations", Activity.Other),
            Map.entry("Data Sources", Activity.LocalConfiguration),
            Map.entry("Server Explorer", Activity.LocalConfiguration),
            Map.entry("NCrunch Configuration", Activity.LocalConfiguration),
            Map.entry("Python Environments", Activity.LocalConfiguration),
            Map.entry("Templates Explorer", Activity.LocalConfiguration),
            Map.entry("Zeiterfassung", Activity.ProjectManagement),
            Map.entry("Source Not Available", Activity.Other),
            Map.entry("Source Not Found", Activity.Other),
            Map.entry("Properties", Activity.Development),
            Map.entry("Refactoring.+", Activity.Development),
            Map.entry("Regex Tester",Activity.Development)

    );

    private static boolean isMainWindowEvent(WindowEvent e){
        return "vsWindowTypeMainWindow".equals(e.Window.getType());
    }

    private static boolean isProjectManagementWindow(String name){
        return workItemIndicators.stream().anyMatch(name::contains);
    }

    private static Optional<Activity> getToolWindowActivity(String caption){
        //Return an activity if one matches one of the regexes in toolWindowMapping
        return toolWindowMapping.entrySet()
                .stream()
                .filter(entry-> Pattern.compile(entry.getKey()).matcher(caption).matches())
                .findFirst()
                .map(Map.Entry::getValue);
    }

    @Override
    public Optional<Activity> map(WindowEvent event) {

        if(isMainWindowEvent(event)){
            return Optional.of(event.Action == WindowAction.Activate?Activity.EnterIDE:Activity.LeaveIDE);
        }

        if(event.Action == WindowAction.Move || event.Action == WindowAction.Create || event.Action == WindowAction.Close){
            return Optional.of(Activity.LocalConfiguration);
        }

        return switch (event.Window.getType()){
            case "vsWindowTypeBrowser" -> Optional.of(Activity.Navigation);
            case "vsWindowTypeDocumentOutline" -> Optional.of(Activity.Navigation);
            case "vsWindowTypeOutput" -> Optional.of(Activity.Development);
            case "vsWindowTypeToolbox" -> Optional.of(Activity.Development);
            case "vsWindowTypeProperties"->Optional.of(Activity.Development);
            case "vsWindowTypeSolutionExplorer"->Optional.of(Activity.Navigation);
            case "vsWindowTypeTaskList" -> Optional.of(Activity.ProjectManagement);
            case "vsWindowTypeDocument" -> Optional.of(isProjectManagementWindow(event.Window.getCaption())?Activity.ProjectManagement:Activity.Navigation);
            case "vsWindowTypeToolWindow" -> getToolWindowActivity(event.Window.getCaption());
            default -> Optional.empty();
        };

    }
}
