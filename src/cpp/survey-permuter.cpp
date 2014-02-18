#include <iostream>
#include <vector>
#include <math.h>

#include "bootstrap.h"
#include "mwc.h"
#include "realrandomvalue.h"
#include "fyshuffle.h"
#include "sortindices.h"

#include "surveyreader.h"

using namespace std;

const auto NUM_RANDOM_RESPONDENTS = 20; // number of "random" respondents
const auto NUM_TRIALS = 1000;   // number of randomized permutation trials
// const float P_VALUE = 0.05;     // for outlier detection

// FIX ME!
const float P_VALUE = 0.5;     // for outlier detection
//const float OUTLIER_MULTIPLIER = 2.0; 
//const float OUTLIER_MULTIPLIER = 10.0;

int
main()
{
  // Seed the RNG with a real random value.
  MWC rng (RealRandomValue::value(), RealRandomValue::value());

  // Read in the survey.
  vector<int> choices;
  vector<vector<int>> survey;
  readSurvey (cin, choices, survey);

  auto const numQuestions     = choices.size();
  auto numChoices = 0;

  // Add in some random responses.
  auto const originalSize = survey.size();
  survey.resize (originalSize + NUM_RANDOM_RESPONDENTS);
  for (int i = 0; i < NUM_RANDOM_RESPONDENTS; i++) {
    for (int j = 0; j < numQuestions; j++) {
      survey[originalSize+i].push_back (rng.next() % choices.at (j));
    }
  }

  auto const numRespondents   = survey.size();

#if 0
  cout << "read in " << originalSize << " responses." << endl;
  cout << "survey size (with random responses inserted at end) = " << numRespondents << endl;
#endif

  // Now let's run trials and compute a histogram of sums of these,
  // but with random permutations of the bit values.

  vector<int> tail (numRespondents);
  vector<vector<int>> perm (numQuestions);
  for (int i = 0; i < numQuestions; i++) {
    //    cout << "size " << i << " = " << choices.at (i) << endl;
    numChoices += choices.at (i) - 1;
    perm.at(i).resize (choices.at (i));
  }

  unsigned long long accumulatedSums = 0;

  for (int z = 0; z < NUM_TRIALS; z++) {

    for (int i = 0; i < numQuestions; i++) {
      for (int j = 0; j < choices.at (i); j++) {
	perm.at(i).at(j) = j;
      }
      fyshuffle::inplace (perm.at (i));
    }
    
    // Now act as if we had a new survey with the responses
    // appropriately transformed.

    vector<float> sums (numRespondents);
    
    for (int i = 0; i < numRespondents; i++) {
      //      cout << "processing question " << i << endl;
      auto sum = 0;
      for (int q = 0; q < numQuestions; q++) {
	auto orig = survey.at(i).at(q);
	//	cout << "orig = " << orig << endl;
	auto permuted = perm.at(q).at(orig);
	//	cout << "permuted = " << permuted << endl;
	sum       += permuted;
      }

      // We add a bit of noise to make it exceedingly unlikely that
      // any two sums are actually identical. This addition of random
      // noise smooths out bias inherent in the sorting algorithm used
      // below.

      //      cout << "num questions = " << numQuestions << endl;
      //      cout << "respondent " << i << ", randomsum = " << randomSum << ", actual sum = " << sum << endl;
      sums[i] = sum + drand48() / 1000.0;
    }

    vector<size_t> sorted_indices = sort_indices (sums);

    for (int i = 0; i < ((float) numRespondents * P_VALUE) / 200; i++) {
      tail[sorted_indices[i]]++;
    }
    for (int i = numRespondents - ((float) numRespondents * P_VALUE) / 200; i < numQuestions; i++) {
      tail[sorted_indices[i]]++;
    }
  }

  /* 
     Chernoff bound, for any positive delta:
     Pr[X > (1+delta)mean] <= e^(-delta^2 mean / (2 + delta)) ]
   */
  auto const n = NUM_TRIALS;
  auto const N = numRespondents;
  auto const p = 1.0 / N;
  auto const mean = n * p;
  auto const variance = n * p * (1.0 - p);

  auto i = 0;
  auto falsePositives = 0;
  auto falseNegatives = 0;
  auto truePositives = 0;
  auto trueNegatives = 0;
  for (auto value : tail) {
    //    cout << "(" << i << ": " << value << ") ";

    auto likelihood = value / mean;
#if 0
    if (value > mean) {

      cout << "value = " << value << endl;
      cout << "mean = " << mean << endl;
      auto delta = (value / mean) - 1.0;
      cout << "delta = " << delta << endl;

      // Markov's inequality.
      // Pr[X > a] < E[X] / a
      cout << "Markov's likelihood < " << mean / value << endl;
      
      // Chebyshev bounds.
      // Pr[|X - mean| >= a] <= variance / a^2
      cout << "Chebyshev's likelihood <" << variance /((value - mean)*(value - mean)) << endl;

      // Chernoff bound.
      // Pr[X >= (1+d)mean] <= exp(mean*-d^2/(2+d))
      likelihood = exp (-delta*delta*mean / (2.0 + delta));
      cout << "Chernoff bound < " << likelihood << endl;

      // Chernoff bound, redux.
      // Pr[X > (1+d)mean] < pow(e^d / pow(1+d,1+d), mean).
      cout << "Chernoff bound2 < " << pow(exp(delta) /
					  pow(1.0+delta,1.0+delta),
					  mean) << endl;
      // If 0 <= delta <= 1, Pr <= exp (-delta^2 / 3 * mean)
#if 0
      // commented out since this bound appears looser than the above.
      if (delta <= 1.0) {
	likelihood = exp (-delta * delta / 3.0 * mean);
	cout << "revised likelihood = " << likelihood << endl;
      }
#endif
    }
#endif

    cout << likelihood << "\t" << i << endl;
    if (value / mean > 1.36) { // FIX ME
      // Marked as outlier.
      if (i < numRespondents - NUM_RANDOM_RESPONDENTS) {
	// Not a random one, so false positive.
	//	cout << "BOGUS OUTLIER!";
	falsePositives++;
      } else {
	//	cout << "REAL OUTLIER!";
	truePositives++;
      }
    } else {
      // Not marked as an outlier but should be.
      if (i >= numRespondents - NUM_RANDOM_RESPONDENTS) {
	falseNegatives++;
      } else {
	trueNegatives++;
      }
    }
    i++;
  }
  float precision =  (float) truePositives / (float) (truePositives + falsePositives);
  float recall    =  (float) truePositives / (float) (truePositives + falseNegatives);
  float fScore    =  2.0 * precision * recall / (precision + recall);

#if 1
  cout << "False positives = " << falsePositives << endl;
  cout << "False negatives = " << falseNegatives << endl;
  cout << "True positives = "  << truePositives << endl;
  cout << "True negatives = "  << trueNegatives << endl;
  cout << "Precision = " << precision << endl;
  cout << "Recall    = " << recall    << endl;
  cout << "F-score   = " << fScore << endl;
  cout << "True negative rate (specificity) = " << (float) trueNegatives / (trueNegatives + falsePositives) << endl;
  cout << endl;
#endif

  return 0;
}
