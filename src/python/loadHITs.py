import csv, os
import make_survey
import evaluation
# read in files in a directory
# parse into a map
# analyze

universal_headers = ['HitId','HitTitle','Annotation','AssignmentId','WorkerId','Status','AcceptTime','SubmitTime']
qrows_lookup = {}
orows_lookup = {}

def get_survey(source_csv):
    return make_survey.parse(source_csv)

def make_row_lookups(survey):
    for question in survey.questions:
        quid = question.quid
        for row in question.sourceRows:
            qrows_lookup[row] = quid
        for o in question.options:
            (r, c) = o.sourceCellId
            orows_lookup["_".join([str(r), str(c)])] = o.oid

def load_from_dir (dirname, survey):
    # model responses as lists, rather than SurveyResponse objects, as
    # in the evaluation namespace
    header = True
    responses = []
    for filename in os.listdir(dirname):
        if 'csv' not in filename:
            continue
        reader = csv.reader(open(dirname+"/"+filename, "rU"))
        # the user response - should have entries for each question
        response = {}
        for row in reader:
            if header:
                headers = row
                header = False
            else:
                answers = row[len(universal_headers):]
                # add actual answers - will be a list of ids
                for ans in answers:
                    try:
                        (joid, qpos, opos) = ans.split(';')
                    except ValueError:
                        continue
                    (_, r, c) = joid.split("_")
                    quid = qrows_lookup[int(r)]
                    oid = orows_lookup["_".join([str(r), str(c)])]
                    response[quid] = (oid, qpos)
        if len(response) == 0:
            continue
        responses.append(response)
        header = True
    return responses

                    
if __name__ == "__main__":
    source = 'data/Ipierotis.csv'
    hitDir = '/Users/etosch/Desktop/ipierotis_results/'
    survey = get_survey(source)
    make_row_lookups(survey)
    responses = load_from_dir(hitDir, survey)
    bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, responses, 0.05)
    bad_q_text = [ q for q in survey.questions if q.quid == bad_q ]
    print("Total questions:", len(survey.questions))
    print("Bad positions:", bad_pos)
    print("Bad questions:", bad_q, ":", bad_q_text)
