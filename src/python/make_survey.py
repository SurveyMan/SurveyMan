# parses survey from source into the python object
from __init__ import *
from survey.objects import *
import csv

# positions of the headers
QUESTION, OPTIONS, RESOURCE, BLOCK, EXCLUSIVE, RANDOMIZE, FREETEXT, ORDERED, BRANCH, CORRELATE = [None]*10

with open(spec) as fp:
    default_headers = {}
    for line in fp.readlines():
        try:
            (h, htype) = line.split("=")
        except ValueError:
            continue
        default_headers[h.strip()] = htype.strip()

def pos(lst, item, default=-1):
    try:
        return lst.index(item)
    except ValueError:
        return default

def set_header_positions(ordered_headers):
    QUESTION = pos(ordered_headers, 'QUESTION')
    OPTIONS = pos(ordered_headers, 'OPTIONS')
    RESOURCE = pos(ordered_headers, 'RESOURCE')
    BLOCK = pos(ordered_headers, 'BLOCK')
    EXCLUSIVE = pos(ordered_headers, 'EXCLUSIVE')
    RANDOMIZE = pos(ordered_headers, 'RANDOMIZE')
    FREETEXT = pos(ordered_headers, 'FREETEXT')
    ORDERED = pos(ordered_headers, 'ORDERED')
    BRANCH = pos(ordered_headers, 'BRANCH')
    CORRELATE = pos(ordered_headers, 'CORRELATE')

def parse(filename):
    reader = csv.reader(filename, 'rU')
    header = True
    for row in reader:
        if header:
            ordered_headers = [s.upper() for s in row]
            set_header_positions(ordered_headers)
            header = False
        else:
            
            
        

def load_survey (filename) :
    """Takes a csv as input and returns a Python Survey object."""
    lexed_map = parse(filename)
    

if __name__ == "__main__":
    print(default_headers)
