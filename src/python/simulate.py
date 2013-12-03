"""Simulate"""

#
# Flip a bunch of coins (N) and see how likely it is that they look
# like they are biased by some percent (percentBias).
#

from random import randint
from math   import log

def entropy(l):
    "Compute the entropy of a list of values."
    N = sum(l)
    ent = 0.0
    for i in l:
        if (i != 0):
            ent += i * log(i * 1.0 / N)
    if (ent == 0.0):
        return 0.0
    else:
        return -ent


def simulate (N = 50,
              coinSides = 4,
              confidence = 0.95,
              simulations = 10000):
    "Simulate coin flips to establish confidence level on entropy."
    simulations = 10000 
    # Pre-compute normalized max entropy.
    maxEntFlips = [(N * 1.0/coinSides) for x in range(0, coinSides)]
    maxEnt = entropy(maxEntFlips)
    # List of normalized entropies, for sorting later to find confidence interval.
    entropies = []
    # Start the simulations.
    for kk in range(1, simulations+1):
        # Initialize the flips array, which counts the occurrences of
        # each "side".
        flips  = [0 for x in range(0, coinSides)]
        # Flip N "coins".
        for i in range(1, N+1):
            v = randint(0, coinSides-1)
            # Add one to the appropriate bin.
            flips[v] += 1
        # Add the normalized entropy.
        entropies.append ((flips, entropy(flips) / maxEnt))

    entropies.sort()
    print "%f%% of entropies greater than %f" % \
    (100 * confidence, entropies[int((1.0 - confidence) * simulations)][1])

    print entropies[int((1.0 - confidence) * simulations)][0]
#    print "biased = %d, fraction = %f"  % (biased, biased * 1.0 / simulations)
#    print "N = %d, expected = %f, biased = %f" % (N, p * N, int(round(bias * N)))
#    print "unbiased fraction = %f%%" % (1.0 - (biased / (1.0 * simulations)))

simulate(N = 50, coinSides = 8, confidence = 0.90)


