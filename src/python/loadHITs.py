import csv
# read in files in a directory
# parse into a map
# analyze

universal_headers = ['HitId','HitTitle','Annotation','AssignmentId','WorkerId','Status','AcceptTime','SubmitTime']
responses = {}

for header in universal_headers:
    responses[header] = []
universal_headers[responses] = {}

def load_from_dir (dir_name):
    header = True
    for filename in os.listdir(dirname):
        reader = csv.reader(open(filename), "rU")
        for row in reader:
            if header:
                headers = row
                header = False
            else:
                answers = row[len(universal_headers):]
                for (i, h) in enumerate(headers[:len(universal_headers)]):
                    responses[h].append(row[i])
                # add actual answers - will be a list of ids
                for ans in answers:
                    (qid, qpos, oid, opos) = ans.split(';')
                    
