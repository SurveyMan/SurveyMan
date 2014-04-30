import unittest
from SurveyMan.survey.survey_representation import *
from SurveyMan.survey.survey_exceptions import *
import SurveyMan.examples.subblock_example as subEx
import SurveyMan.examples.example_survey as exSurv
import SurveyMan.examples.SimpleSurvey as simpSurv
import SurveyMan.examples.TwoBranchesOneBlock as twob1b
import SurveyMan.examples.TwoBranchesOneBlock2 as twob2b2
import SurveyMan.examples.TwoBranchesOneSubblock2 as twob1sub
import SurveyMan.examples.BackwardsBranching as backwards
import SurveyMan.examples.BranchToSubblock as branch2sub


class ConstraintTests(unittest.TestCase):

    def setUp(self):
        self.blockSurvey = subEx.createSurvey()
        self.ipierotisSurvey = exSurv.createSurvey()
        self.simpleSurvey = simpSurv.createSurvey()
        self.brokenBranch = twob1b.createSurvey()
        self.brokenBranch2 = twob2b2.createSurvey()
        self.brokenBranchSubblock = twob1sub.createSurvey()
        self.backwardsBranch = backwards.createSurvey()
        self.branchToSubblock = branch2sub.createSurvey()

    def tearDown(self):
        del self.blockSurvey
        del self.ipierotisSurvey
        del self.simpleSurvey
        del self.brokenBranch
        del self.brokenBranch2 
        del self.brokenBranchSubblock
        del self.backwardsBranch 
        del self.branchToSubblock 

    def testTopLevelBranchCheck(self):
        print("testing top level branch check")
        self.assertRaises(InvalidBranchException, self.branchToSubblock.jsonize)
    
    def testBackwardsBranchCheck(self):
        print("testing backwards branch check")
        self.assertRaises(InvalidBranchException, self.backwardsBranch.jsonize)

    def testBlockBranchNumber(self):
        print("testing block paradigm check")
        #check that survey with invalid BranchAll throws exception
        self.assertRaises(InvalidBranchException, self.brokenBranch.jsonize)
        self.assertRaises(InvalidBranchException, self.brokenBranch2.jsonize)
        self.assertRaises(InvalidBranchException, self.brokenBranchSubblock.jsonize)
        #check that surveys with valid/no branching throw no exceptions
        self.blockSurvey.jsonize() #not valid?
        self.ipierotisSurvey.jsonize()
        self.simpleSurvey.jsonize()

if __name__=='__main__':
    unittest.main()
