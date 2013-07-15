#include <cassert>
#include <iostream>
#include <vector>
#include <algorithm>
#include <string>
#include <sstream>

#include "fyshuffle.h"
#include "realrandomvalue.h"
#include "stats.h"

template <typename T>
vector<size_t> sort_indexes(const vector<T> &v) {
  // From StackOverflow.
  // Sorts indices rather than values.

  // Initialize original index locations.
  vector<size_t> idx(v.size());
  for (size_t i = 0; i != idx.size(); ++i) idx[i] = i;

  // Sort indexes based on comparing values in v.
  sort(idx.begin(), idx.end(),
       [&v](size_t i1, size_t i2) {return v[i1] < v[i2];});

  return idx;
}

///
// Analysis for surveys over categorical data (discrete, unordered),
// with a single response per question.  We assume that questions are
// presented to respondents in random order.

using namespace std;

/// @brief Read in a csv file containing the survey (from stdin).
void readSurvey (istream& in,
		 vector<int>& header,
		 vector<vector<int>>& survey)
{
  // The first line contains the number of choices for each question.
  // The rest of the lines contain responses.

  // Read in and parse the first line.
  string line;
  getline (in, line);
  stringstream lineStream (line);
  string cell;

  while (getline (lineStream, cell, ',')) {
    int value;
    istringstream (cell) >> value;
    header.push_back (value);
  }

  // Now read in the rest of the survey.
  while (getline (in, line)) {

    stringstream lineStream (line);
    string cell;

    vector<int> responses;
    while (getline (lineStream, cell, ',')) {
      int value;
      istringstream (cell) >> value;
      responses.push_back (value);
    }
    survey.push_back (responses);
  }
}


int main()
{
  enum { NUM_TRIALS = 100000 }; // The number of randomized trials to perform.

  srand48 (RealRandomValue::value()); // Seed the RNG with a real random number.

  // Read in the survey.
  vector<int> header;
  vector<vector<int>> theSurvey;
  readSurvey (cin, header, theSurvey);

  auto const numQuestions = header.size();
  auto const numSurveys   = theSurvey.size();

  cout << "Read in " << numSurveys << " responses." << endl;

#if 0
  // Generate a certain number of randomized responses and add them to
  // the survey.
  auto const numRandom = 10;
  vector<int> sums;
  //  sums.resize (numRandom);
  for (int i = 0; i < numRandom; i++) {
    int s = 0;
    for (int q = 0; q < numQuestions; q++) {
      // responses.push_back (lrand48() % header[q]);
      long l = lrand48() % header[q];
      cout << l << ",";
      // cout << "l = " << l % header[q] << ", header[" << q << "] = " << header[q] << endl;
    }
    cout << endl;
  }
#endif

  // Compute the expected sum of responses for a random respondent.
  float randomAverage;
  {
    int v = 0;
    for (int i = 0; i < numQuestions; i++) {
      v += header[i] - 1;
    }
    randomAverage = v / 2.0;
  }

  // Build the count matrix, which we use to find outliers. 
  vector<vector<int>> count;
  count.resize (numSurveys);
  for (auto& s : count) {
    s.resize (numSurveys);
  }

  // Repeatedly run trials where we generate random re-mappings of the
  // questions and compute sums.

  for (int k = 0; k < NUM_TRIALS; k++) {

    // Remap the surveys based on random permutations.

    // First, build a random permutation for each question.
    vector<vector<int>> perm;
    perm.resize (numQuestions);
    for (int i = 0; i < numQuestions; i++) {
      perm[i].resize (header[i]);
      for (int j = 0; j < header[i]; j++) {
	perm[i][j] = j;
      }
      fyshuffle::inplace (perm[i]);
    }

    // Now rebuild a new survey with the responses appropriately
    // transformed.
    vector<float> sums;
    sums.resize (numSurveys);

    auto sm = theSurvey;

    for (int i = 0; i < numSurveys; i++) {
      sm[i] = theSurvey[i];

      for (int j = 0; j < sm[i].size(); j++) {
	int oldAnswer = sm[i][j];
	int newAnswer = perm[j][oldAnswer];
	sm[i][j] = newAnswer;
      }

      sums[i] = 0;

      for (auto v : sm[i]) {
	sums[i] += v;
      }
    }

    // Subtract the random average so the mean is 0.
    for (int i = 0; i < numSurveys; i++) {
      sums[i] -= randomAverage;
    }

    auto origSums = sums;

    sort (sums.begin(), sums.end());

#if 0
    for (auto s : sums) {
      cout << s << " ";
    }
    cout << endl;
#endif

    assert (origSums.size() == numSurveys);
    assert (sums.size() == numSurveys);

    // See where each question ended up in the array of sums.
    for (int i = 0; i < numSurveys; i++) {
      for (int k = 0; k < numSurveys; k++) {
	if (origSums[i] == sums[k]) {
	  count[i][k]++;
	  break;
	}
      }
    }
  }

  float expectedCount = 2.0 / (float) numSurveys;

  for (int i = 0; i < numSurveys; i++) {
#if 0
    for (int k = 0; k < numSurveys; k++) {
      cout << count[i][k] << " ";
    }
    cout << endl;
#endif

    float percentAtTails = (count[i][0] + count[i][numSurveys-1]) / (float) NUM_TRIALS;
    cout << "Question " << i << ":" << " Z-score = " << stats::average (count[i]) / stats::stddev (count[i]) << endl;
    cout << "  % at tails = " << percentAtTails;

    // Print stars after apparent outliers.
    if (percentAtTails > 2.0 * expectedCount) {
      cout << "****";
    }
    cout << endl;
#if 0
    for (int k = 0; k < numSurveys; k++) {
      cout << count[i][k] << " ";
    }
    cout << endl;
#endif
  }

  cout << "expected count = " << expectedCount << endl;
  return 0;
}
