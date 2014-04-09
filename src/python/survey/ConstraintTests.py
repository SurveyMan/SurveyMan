import unittest
from survey_representation import *
from survey_exceptions import *
import subblock_example
import example_survey
import SimpleSurvey
import TwoBranchesOneBlock


class ConstraintTests(unittest.TestCase):

    def setUp(self):
        self.blockSurvey = subblock_example.createSurvey()
        self.ipierotisSurvey = example_survey.createSurvey()
        self.simpleSurvey = SimpleSurvey.createSurvey()
        self.brokenBranch = TwoBranchesOneBlock.createSurvey()

    def tearDown(self):
        del self.blockSurvey
        del self.ipierotisSurvey
        del self.simpleSurvey
        del self.brokenBranch

    def testBlockCheck(self):
        self.assertRaises(InvalidBranchException, self.brokenBranch.jsonize)

if __name__=='__main__':
    unittest.main()
