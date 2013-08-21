package gui.actionmanager;


import gui.HITActions;
import system.mturk.SurveyPoster;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HITAction implements ActionListener {
    private HITActions action;
    public HITAction(HITActions action) {
        this.action = action;
    }
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        switch (action) {
            case EXPIRE:
                SurveyPoster.expireOldHITs();
                break;
            case DELETE:
                SurveyPoster.deleteExpiredHITs();
                break;
        }
    }
}
