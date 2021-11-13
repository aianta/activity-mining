package com.activity.mining.mappers;

import cc.kave.commons.model.events.CommandEvent;
import com.activity.mining.Activity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/** Ported from Kave.FeedbackProcessor CommandEventToActivityMapper.cs
 *
 */

public class CommandEventMapper implements EventToActivityMapper<CommandEvent> {

    //Logging
    private static final Logger log = LoggerFactory.getLogger(CommandEventMapper.class);

    private static Set<String> waitingCommands = Set.of(
            "{BC6F0E30-528C-4EEA-BC2E-C6B35E589068}:265:FortifyToolMenu.ExportAuditProject",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:331:File.SaveSelectedItems",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:882:Build.BuildSolution",
            "{5BF14E63-E267-4787-B20B-B814FD043B38}:21014:File.TfsCheckIn",
            "{FFE1131C-8EA1-4D05-9728-34AD4611BDA9}:6200:TeamFoundationContextMenus.SourceControlExplorer.TfsContextExplorerCloak",
            "{5BF14E63-E267-4787-B20B-B814FD043B38}:21009:ClassViewContextMenus.ClassViewProject.SourceControl.TfsContextUndoCheckout",
            "{FFE1131C-8EA1-4D05-9728-34AD4611BDA9}:6356:TeamFoundationContextMenus.PendingChangesPageChangestoInclude.TfsContextPendingChangesPageUndo",
            "{5BF14E63-E267-4787-B20B-B814FD043B38}:21008:File.TfsUndoCheckout",
            "{FFE1131C-8EA1-4D05-9728-34AD4611BDA9}:4653:File.TfsFindChangesets",
            "{5BF14E63-E267-4787-B20B-B814FD043B38}:21010:File.TfsGetLatestVersion",
            "{15061D55-E726-4E3C-97D3-1B871D9B5AE9}:20483:Team.GotoWorkItem",
            "{BC6F0E30-528C-4EEA-BC2E-C6B35E589068}:260:FortifyToolMenu.GenerateReport",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:295:Debug.Start",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:249:Debug.StepOver",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:213:Debug.AttachtoProcess",
            "{3A680C5B-F815-414B-AA4A-0BE57DADB1AF}:512:Debug.ReAttach",
            "{5EFC7975-14BC-11CF-9B2B-00AA00573819}:179:Debug.StopDebugging",
            "CleanupCode",
            "Generate",
            "AnalyzeReferences",
            "FindUsages",
            "Template1",
            "SilentCleanupCode",
            "RefactorThis",
            "GotoDeclarationShort"
    );

    private final Map<String, Activity> commandIdToActivityMapping = new HashMap<>();

    //TODO: This regex could be faulty
    private static final Pattern recentFileCommandRegex = Pattern.compile("^[0-9] (?:[a-zA-Z]\\:|\\\\\\\\[\\w\\.]+\\\\[\\w.$]+)\\\\(?:[\\w]+\\\\)*\\w([\\w.])+$");

    private static boolean isOpenRecentFileCommand(String commandId){
        return recentFileCommandRegex.matcher(commandId).matches();
    }

    public CommandEventMapper(String csvMappings){
        loadCSV(csvMappings);
    }

    /**
     * Load commandId to activity mappings from a csv file.
     * Populates the commandIdToActivityMapping HashMap.
     * @param csvMappings
     */
    private void loadCSV(String csvMappings){
        try(Reader in = new FileReader(csvMappings)){
            Iterable<CSVRecord> records = CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setRecordSeparator("\n")
                    .setHeader("Command Id","Activity")
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(in);

            StreamSupport.stream(records.spliterator(), false)
                    .forEach(entry->{
                        try {
                            commandIdToActivityMapping.put(
                                    entry.get("Command Id"),
                                    Activity.fromShorthand(entry.get("Activity")));
                        } catch (Exception e) {
                            log.error(e.getMessage(),e);
                        }
                    });
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Activity> map(CommandEvent event) {

        String commandId = event.CommandId;

        if (isOpenRecentFileCommand(commandId)){
            return Optional.of(Activity.Navigation);
        }

        if (commandIdToActivityMapping.containsKey(commandId)){
            return Optional.ofNullable(commandIdToActivityMapping.get(commandId));
        }

        if (commandId.toLowerCase().contains("tfs")){
            return Optional.of(Activity.ProjectManagement);
        }

        if (waitingCommands.contains(commandId)){
            return Optional.of(Activity.Waiting);
        }

        return Optional.of(Activity.Other);
    }


}
