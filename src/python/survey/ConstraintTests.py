import unittest
from survey_representation import *
from survey_exceptions import *
import subblock_example
import example_survey
import SimpleSurvey
import TwoBranchesOneBlock
import BackwardsBranching
import BranchToSubblock


class ConstraintTests(unittest.TestCase):

    def setUp(self):
        self.blockSurvey = subblock_example.createSurvey()
        self.ipierotisSurvey = example_survey.createSurvey()
        self.simpleSurvey = SimpleSurvey.createSurvey()
        self.brokenBranch = TwoBranchesOneBlock.createSurvey()
        self.backwardsBranch = BackwardsBranching.createSurvey()
        self.branchToSubblock = BranchToSubblock.createSurvey()

    def tearDown(self):
        del self.blockSurvey
        del self.ipierotisSurvey
        del self.simpleSurvey
        del self.brokenBranch

    def testTopLevelBranchCheck(self):
        self.assertRaises(InvalidBranchException, self.branchToSubblock.jsonize)
    
    def testBackwardsBranchCheck(self):
        self.assertRaises(InvalidBranchException, self.backwardsBranch.jsonize)

    def testBlockBranchNumber(self):
        #check that survey with invalid BranchAll throws exception
        self.assertRaises(InvalidBranchException, self.brokenBranch.jsonize)
        #check that surveys with valid/no branching throw no exceptions
        self.blockSurvey.jsonize() #not valid?
        self.ipierotisSurvey.jsonize()
        self.simpleSurvey.jsonize()

if __name__=='__main__':
    unittest.main()
