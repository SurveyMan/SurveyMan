__doc__ = """ Driver loop for simulating survey responses."""
import sys

if __name__=="__main__":
    if len(sys.argv)==1:
        print("Please provide the path to the survey csv you wish to profile:")
        filename = sys.stdin.readline()
    else: 
        filename = sys.argv[1]
    print("Parsing file into a survey object...", end="")
    # read the file into a survey object
    survey = parse_file(filename)
    print("done")
    print("For each question, specify the number of 
