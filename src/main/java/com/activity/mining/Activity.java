package com.activity.mining;

import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.model.events.visualstudio.BuildEvent;

/**
 *  Replicated from https://github.com/kave-cc/csharp-data-processing/blob/a54cc61e243cf01425c00e6a7694eda04c33766d/KaVE.FeedbackProcessor/Activities/Model/Activity.cs
 */

public enum Activity {

    Any,
    Development,
    ProjectManagement,
    LocalConfiguration,
    Navigation,
    //Testing, //There is no testing activity in the paper... this would be a deviation.
    Other,

    //Inactivities
    Away,
    Inactive,
    InactiveLong,
    Waiting,

    //Legacy
    EnterIDE,
    LeaveIDE;

    public static Activity fromShorthand(String shortcode) throws Exception {
        return switch (shortcode){
            case "D" -> Activity.Development;
            case "U" -> Activity.Navigation;
            case "PM" -> Activity.ProjectManagement;
            case "LC" -> Activity.LocalConfiguration;
            case "" -> Activity.Other;
            case "N" -> Activity.Navigation;
            case "VC"-> Activity.ProjectManagement; //Version control falls under project management
            case "E" -> Activity.Development; //I see 'E' in the csv a lot, it looks to mean editing, so I'll categorize it as development.
            case "T" -> Activity.Development; //T seems to refer to testing, but there is no testing activity in the paper...
            case "O" -> Activity.Other;
            default -> throw new Exception("Unknown activity shorthand: " + shortcode);
        };
    }

}
