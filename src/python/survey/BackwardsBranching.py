#example of invalid branching
#based on https://github.com/etosch/SurveyMan/blob/master/data/tests/test4_two_branches_one_block.csv
from survey_representation import *

def createSurvey():
    q1 = Question("radio","Question 1",[Option("foo"),Option("bar"),Option("baz")])
    q2 = Question("radio","Question 2",[Option("boo"),Option("far"),Option("faz")])
    q3 = Question("radio","Question 3",[Option("eggs"),Option("ham")])

    block1 = Block([q1,q2,q3])

    q4 = Question("radio","Question 4",[Option("oscar"),Option("lucille"),Option("george")])
    q5 = Question("radio","Question 5",[Option("maeby"),Option("george")])
    q6 = Question("radio","Question 6",[Option("gob"),Option("lindsay")])
    q7 = Question("radio","Question 7",[Option("anne veal"),Option("gene parmesean")])	

    block2 = Block([q4,q5,q6,q7])

    #didn't add all options
    q8 = Question("radio","Question 8",[Option("lupe"),Option("marky mark"),Option("tony wonder")])
    q9 = Question("radio","Question 9",[Option("whooopsie"),Option("daisy")])

    block3 = Block([q8,q9])

    q10 = Question("radio","Her?",[])

    block4 = Block([q10])

    q11 = Question("radio","Would you mind telling us how this survey made you feel?",[])

    block5 = Block([q11])

    q12 = Question("radio","Did someone say wonder?",[])

    block6 = Block([q12])

    branch1 = Constraint(q3)
    branch1.addBranchByOpText("eggs",block2)
    branch1.addBranchByOpText("ham",block3)

    #branch backwards
    branch2 = Constraint(q7)
    branch2.addBranchByIndex(0,block1)
    branch2.addBranchByIndex(1,block1)


    blockList=[block1,block2,block3,block4,block5,block6]
    branchList=[branch1,branch2]

    survey = Survey(blockList,branchList)

    return survey
def main():
    survey1 = createSurvey()
    #survey1.jsonize()

if __name__ == '__main__':
    main()
