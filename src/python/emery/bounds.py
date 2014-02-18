"""
   bounds: confidence interval calculation for bot detection.
   @author Emery Berger <http://www.cs.umass.edu/~emery>

"""

# TODO: Things to handle:
#   branches
#   randomized but ordered options

from random import seed,randint

seed()

def computeCI (N = 50,
               coinSides = -1,
               confidence = 0.95,
               simulations = 10000):
    "Simulate coin flips to establish confidence intervals."
    if (coinSides == -1):
        # Not specified: default to two-sided coins.
        coinSides = [2 for x in range(N)]
    sums = []
    # Start the simulations.
    for kk in range(1, simulations+1):
        sum = 0
        # Flip N "coins".
        for i in range(0, N):
            v = randint(0, coinSides[i]-1)
            sum += v 
        sums.append (sum)

    sums.sort()

    index = int((1.0 - confidence) * simulations)

    return (sums[index], sums[simulations-index])


(left, right) = computeCI (N = 10,
                           coinSides=[2,2,4,5,4,2,3,4,4,2],
                           confidence = 0.95,
                           simulations = 10000)

print "%f confidence interval is (%f, %f)." % (0.95, left, right)
