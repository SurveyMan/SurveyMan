from __init__ import *
import csv, os, sys
import make_survey
import evaluation
# read in files in a directory
# parse into a map
# analyze

universal_headers = ['HitId','HitTitle','Annotation','AssignmentId','WorkerId','Status','AcceptTime','SubmitTime']

def get_survey(source_csv):
    return make_survey.parse(source_csv)


def load_from_dir (dirname, survey):
    # model responses as lists, rather than SurveyResponse objects, as
    # in the evaluation namespace

    qrows_lookup = {}
    orows_lookup = {}
    for question in survey.questions:
        for row in question.sourceRows:
            assert(row not in qrows_lookup)
            qrows_lookup[row] = question
        for o in question.options:
            (row, _) = o.sourceCellId
            assert(row not in orows_lookup)
            orows_lookup[row] = o

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
                response['WorkerId'] = row[headers.index('WorkerId')]
                response['AssignmentId'] = row[headers.index('AssignmentId')]
                response['Answers'] = {}
                answers = row[len(universal_headers):]
                # add actual answers - will be a list of ids
                for ans in answers:
                    try:
                        (joid, qpos, opos) = ans.split(';')
                    except ValueError:
                        continue
                    (_, r, c) = joid.split("_")
                    q = qrows_lookup[int(r)]
                    o = orows_lookup[int(r)]

                    # The asserts here caused some weirdness - o changed.
                    #print(o, o.otext)
                    #assert(o.oid in [o.oid for o in q.options])
                    #assert(q not in response['Answers'])
                    #print(o, o.otext, qpos, opos, ans)

                    if 'definitely' in o.otext:
                        assert( opos=='0' or opos=='3' )
                    if 'probably' in o.otext:
                        assert( opos=='1' or opos=='2')
                    response['Answers'][q] = (o, qpos, opos)
        if len(response) == 0:
            continue
        responses.append(response)
        header = True
    return responses

                    
if __name__ == "__main__":
    source = sys.argv[1] #'data/SMLF5.csv'
    hitDir = sys.argv[2] #'/Users/etosch/Desktop/phonology2/'
    survey = get_survey(source)
    responses = load_from_dir(hitDir, survey)
    responses_by_id = [{ q.quid : (o.oid, a, b) for (q, (o, a, b)) in response['Answers'].items() } for response in responses]
    bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, responses_by_id, 0.05)
    bad_qs = [ q for q in survey.questions if q.quid in [bq['question'] for bq in bad_q]]
    print("Total questions:", len(survey.questions))
    print("Bad positions:", bad_pos)
    print("Bad questions (", len(bad_q), " total):")
    for q in bad_qs:
        print("\tScore: ", [e['score'] for e in bad_q if e['question'] == q.quid][0], "\tQuestion:", q)
    print("------------------------------------------------------------------------------------------------")
    print("WorkerId, AssignmentId, Question, Response, Question Position, Response Position")
    for response in responses:
        for (q, (o, qpos, opos)) in response['Answers'].items():
            print(",".join([response['WorkerId'], response['AssignmentId'], "\""+q.qtext+"\"", "\""+o.otext+"\"", qpos, opos]))
        
