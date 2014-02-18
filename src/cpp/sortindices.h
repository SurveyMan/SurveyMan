#ifndef SORTINDICES_H
#define SORTINDICES_H

template <typename T>
vector<size_t> sort_indices (const vector<T>& v) {
  // From StackOverflow.
  // Sorts indices rather than values.

  // Initialize original index locations.
  vector<size_t> idx(v.size());
  for (size_t i = 0; i != idx.size(); ++i) idx[i] = i;

  // Sort indexes based on comparing values in v.
  sort (idx.begin(), idx.end(),
       [&v](size_t i1, size_t i2) {return v[i1] < v[i2];});

  // idx[i] = position of ith element in sorted vector.
  return idx;
}

#endif
