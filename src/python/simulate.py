"""Simulate"""

#
# Flip a bunch of coins (N) and see how likely it is that they look
# like they are biased by some percent (percentBias).
#

from random import randint
from math   import factorial, log

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

def simulate(N = 50,            # number of coins (= # of questions)
             coinSides = 4,     # number of sides to each coin (= # of answers per question)
             percentBias = 40,  # % bias over expected
             conf = 0.95):      # desired confidence level

    p = 1.0 / coinSides          # probability of heads if unbiased
    bias = (p * (1.0 + percentBias / 100.0))  # % chance of heads
    biasNum = int(round(bias * N))

    # Let's compute the likelihood empirically (via Monte Carlo
    # simulation) that N coin flips yields a biased number of heads (given
    # as "bias" % above).

    simulations = 10000 # number of simulations to run
    totalBiased = 0    # number of simulations that appear biased at conf level
   
    biased = 0
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
        # Find the maximum entry.
        maximum = reduce(max, flips)
        # If the max was the biased amount or larger, count the throw as biased.
        if (maximum >= int(round(bias * N))):
            biased += 1

    entropies.sort()
    print "%f%% of entropies higher than %f" % (100 * conf, entropies[int((1.0 - conf) * simulations)][1])
    print entropies[int((1.0 - conf) * simulations)][0]
    print "biased = %d, fraction = %f"  % (biased, biased * 1.0 / simulations)
    print "N = %d, expected = %f, biased = %f" % (N, p * N, int(round(bias * N)))
    print "unbiased fraction = %f%%" % (1.0 - (biased / (1.0 * simulations)))

simulate(N = 50, coinSides = 2)


