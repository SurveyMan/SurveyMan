package gui.actionmanager;

import gui.display.Experiment;
import qc.QC;
import survey.Survey;
import system.mturk.ResponseManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class StatusAction implements ActionListener{
    private Survey survey;

    public StatusAction (Survey survey) {
        this.survey = survey;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try{
            ResponseManager.Record record = ResponseManager.getRecord(survey);
            int totalPosted = record.getAllHITs().length;
            int responsesSoFar = record.responses.size();
            int stillLive = ResponseManager.listAvailableHITsForRecord(record).size();
            boolean complete = QC.complete(record.responses, record.parameters);
            String hitId = record.getLastHIT().getHITId();
            Experiment.updateStatusLabel(String.format("Status of survey %s with id %s:" +
                    "\n\tTotal surveys posted: %d" +
                    "\n\t#/responses so far: %d" +
                    "\n\t#/surveys still live: %d" +
                    "\n\tlast HIT posted: %s" +
                    "\n\tsurvey complete: %b"
                    , survey.sourceName
                    , survey.sid
                    , totalPosted
                    , responsesSoFar
                    , stillLive
                    , hitId
                    , complete
            ));
        } catch (IOException io) {
        }
    }
}
