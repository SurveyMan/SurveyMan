package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import org.apache.log4j.Logger;
import qc.QC;
import survey.SurveyResponse;


class QCAction {

    public static final Logger LOGGER = Logger.getLogger(QCAction.class);

    public static boolean addAsValidResponse(QC.QCActions[] actions, Assignment a, Record record) {
        boolean valid = false;
        for (QC.QCActions action : actions) {
            synchronized (ResponseManager.service) {
                switch (action) {
                    case REJECT:
                        System.out.println("REJECT");
                        //ResponseManager.service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                        System.out.println("(but actually approve for now) - REMOVE ME LATER.");
                        LOGGER.info(String.format("Rejected assignment %s from worker %d", a.getAssignmentId(), a.getWorkerId()));
                        ResponseManager.service.approveAssignment(a.getAssignmentId(), "");
                        valid = false;
                        break;
                    case BLOCK:
                        System.out.println("BLOCK");
                        ResponseManager.service.blockWorker(a.getWorkerId(), QC.BOT);
                        LOGGER.info(String.format("Blocked worker %s", a.getWorkerId()));
                        valid = false;
                        break;
                    case APPROVE:
                        System.out.println("APPROVE");
                        LOGGER.info(String.format("Approved assignment %s from worker %d", a.getAssignmentId(), a.getWorkerId()));
                        if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                            ResponseManager.service.approveAssignment(a.getAssignmentId(), "Thanks.");
                            valid = true;
                        } else valid = false;
                        break;
                    case DEQUALIFY:
                        LOGGER.info(String.format("Revoking qualification for worker %d", a.getWorkerId()));
                        ResponseManager.service.revokeQualification(
                                record.qualificationType.getQualificationTypeId()
                                , a.getWorkerId()
                                , "We are unable to compensate a worker for completing more than one of the same survey for the same study. Thank you for your participation."
                            );
                }
            }
        }
        return valid;
    }
}
