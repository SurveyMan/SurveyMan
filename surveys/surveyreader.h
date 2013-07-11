#ifndef SURVEYREADER_H
#define SURVEYREADER_H

#include <string>
#include <iostream>
#include <sstream>
#include <vector>

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

#endif
