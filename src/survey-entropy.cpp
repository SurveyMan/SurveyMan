//from Emery Berger's experimental folder, entropy algorithm implemented in metrics.py

#include <iostream>
#include <vector>
#include <algorithm>
#include <numeric>
#include <cmath>

#include "bootstrap.h"
#include "mwc.h"
#include "realrandomvalue.h"
#include "fyshuffle.h"
#include "sortindices.h"
#include "stats.h"
#include "surveyreader.h"

using namespace std;

const auto numBootstraps = 100000;
const auto thresholdT = 5;
 
/// @brief Return the entropy of the actual survey responses.
double surveyEntropy (const vector<int>& questions,
		      const vector<vector<int>>& hist)
{
  auto entropy = 0.0;
  // Go through all the questions, using the histogram of responses.
  for (int i = 0; i < questions.size(); i++) {
    auto responseSum = accumulate (hist[i].begin(), hist[i].end(), 0.0);
    // Compute probabilities for each choice.
    for (int j = 0; j < questions[i]; j++) {
      if (hist[i][j] > 0) {
		auto prob = (double) hist[i][j] / responseSum;
		entropy += prob * log(prob);
      }
    }
  }
  return -entropy;
}

/// @brief Build a histogram of responses for each question and choice.
void buildHistogram (const vector<int>& questions,
		     const vector<vector<int>>& survey,
		     vector<vector<int>>& hist)
{
  hist.clear();
  hist.resize (questions.size());
  for (int i = 0; i < questions.size(); i++) {
    hist[i].resize(questions[i]);
  }
  for (int i = 0; i < survey.size(); i++) {
    for (int j = 0; j < questions.size(); j++) {
      hist[j][survey[i][j]]++;
    }
  }
}

/// @brief Print out the histogram.
void printHistogram (ostream& os,
		     const vector<int>& questions,
		     const vector<vector<int>>& hist) 
{
  for (int i = 0; i < questions.size(); i++) {
    os << "hist[" << i << "]: ";
    for (int j = 0; j < questions[i]; j++) {
      os << hist[i][j] << " ";
    }
    os << endl;
  }
}

int
main()
{
  // Seed the RNG with a real random value.
  srand48 (RealRandomValue::value());

  // Read in the survey.
  vector<int> questions;
  vector<vector<int>> survey;
  readSurvey (cin, questions, survey);

  auto nonRandomized = survey.size();

#if 1
  // Add in the same number of random survey respondents.
  int n = survey.size();
  for (int i = 0; i < n; i++) {
    vector<int> responses;
    for (int j = 0; j < questions.size(); j++) {
      responses.push_back (lrand48() % questions[j]);
    }
    survey.push_back (responses);
  }
#endif

  vector<vector<int>>    hist (questions.size());
  vector<vector<double>> perRespondentEntropy (survey.size());
  vector<double>         overallEntropy;

  // Multiplier to ensure that we get close to numBootstraps samples
  // per item.
  const double fraction = (double)survey.size()/(survey.size()-1);
  const double multiplier = pow (fraction, (int) survey.size());

  vector<vector<int>> newSurvey (survey.size());
  vector<bool>        included (survey.size());
 
  // Fire up the bootstraps.
  for (int i = 0; i < numBootstraps * multiplier; i++) {

    // Bootstrap up one new survey.
    bootstrap::completeTracked (survey, newSurvey, included);

    // Build the histogram of responses.
    buildHistogram (questions, newSurvey, hist);

    // Compute the entropy for the survey based on the histogram.
    auto currentEntropy = surveyEntropy (questions, hist);

    overallEntropy.push_back (currentEntropy);

    // Now go through the included array and add the entropy for any
    // item that was NOT included.
    for (auto respondent = 0; respondent < newSurvey.size(); respondent++) {
      if (!included[respondent]) {
	perRespondentEntropy[respondent].push_back (currentEntropy);
      }
    }
  }

  auto n1 = overallEntropy.size();
  auto x1 = accumulate (overallEntropy.begin(), overallEntropy.end(), 0.0) / n1;
  double s1 = stats::stddev (overallEntropy);

  // Go through the non-randomized respondents.
  for (auto i = 0; i < nonRandomized; i++) {

    // Calculate sample sizes, means, and standard deviations.
    auto n2 = perRespondentEntropy[i].size();

    auto x2 =
      accumulate (perRespondentEntropy[i].begin(), perRespondentEntropy[i].end(), 0.0)
      / n2;
    double s2 = stats::stddev (perRespondentEntropy[i]);

    // Welch's t-test =
    //    (mean(x$V1)-mean(z$V1))/sqrt(var(x$V1)/length(x$V1)+var(z$V1)/length(z$V1))    
    auto t = (x1 - x2) / sqrt((s1*s1)/n1 + (s2*s2)/n2);
    if (t > thresholdT) {
      cout << "outlier: " << i << endl;
    }

  }

  return 0;
}
