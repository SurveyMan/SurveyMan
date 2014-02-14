package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import org.apache.log4j.Logger;
import qc.QC;
import survey.SurveyResponse;
import system.Record;


public class QCAction {

    public enum BonusPolicy { EVERY_TWO; }

    public static final Logger LOGGER = Logger.getLogger(QCAction.class);
    public static final String PARTIAL = "Payment for partial completion of our survey";

    public static boolean addAsValidResponse(QC.QCActions[] actions, Assignment a, Record record, SurveyResponse sr) {
        boolean valid = false;
        if (a.getAssignmentStatus().equals(AssignmentStatus.Approved) || a.getAssignmentStatus().equals(AssignmentStatus.Rejected))
            return false;
        for (QC.QCActions action : actions) {
            synchronized (SurveyPoster.service) {
                switch (action) {
                    case REJECT:
                        assert(!a.getAssignmentStatus().equals(AssignmentStatus.Approved));
                        System.out.println("REJECT");
                        LOGGER.info(String.format("Rejected assignment %s from worker %s", a.getAssignmentId(), a.getWorkerId()));
                        SurveyPoster.service.rejectAssignment(a.getAssignmentId(), sr.msg);
                        a.setAssignmentStatus(AssignmentStatus.Rejected);
                        valid = false;
                        break;
                    case BLOCK:
                        System.out.println("BLOCK");
                        SurveyPoster.service.blockWorker(a.getWorkerId(), sr.msg);
                        LOGGER.info(String.format("Blocked worker %s", a.getWorkerId()));
                        a.setAssignmentStatus(AssignmentStatus.Rejected);
                        valid = false;
                        break;
                    case APPROVE:
                        System.out.println("APPROVE");
                        if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                            SurveyPoster.service.approveAssignment(a.getAssignmentId(), "Thanks.");
                            valid = true;
                            a.setAssignmentStatus(AssignmentStatus.Approved);
                            LOGGER.info(String.format("Approved assignment %s from worker %s", a.getAssignmentId(), a.getWorkerId()));
                        } else valid = false;
                        //rewardBonuses(BonusPolicy.EVERY_TWO, a, sr);
                        break;
                    case DEQUALIFY:
                        LOGGER.info(String.format("Revoking qualification for worker %s", a.getWorkerId()));
//                        ResponseManager.service.updateQualificationScore(
//                            record.qualificationType.getQualificationTypeId()
//                            , a.getWorkerId()
//                            , 1
//                        );
                }
            }
        }
        return valid;
    }

    public static void rewardBonuses(BonusPolicy bonus, Assignment a, SurveyResponse sr) {
        switch (bonus) {
            case EVERY_TWO:
                double pay = Math.ceil(sr.responses.size() / 2.0) / 10.0;
                SurveyPoster.service.grantBonus(a.getWorkerId(), pay, a.getAssignmentId(), PARTIAL);
                break;
        }
    }
}
