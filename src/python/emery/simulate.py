"""Simulate"""

#
# Flip a bunch of coins (N) and see how likely it is that they look
# like they are biased by some percent (percentBias).
#

from random import randint
from math   import log

def entropy(l):
    "Compute the entropy of a list of values."
    N = sum(l) # etosch : isn't this just coinSides*(N*1.0/coinSides) = total number of questions?
    ent = 0.0
    for i in l:
        if (i != 0):
            ent += i * log(i * 1.0 / N) # i'm guessing that i is supposed to be the prob (e.g. 1/coinSides) why are we multiplying by N? why isn't this i * log(i)?
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
    # etosch : this is computing a list of length coinSides where the entire contents
    # are the same. 
    maxEntFlips = [(N * 1.0/coinSides) for x in range(0, coinSides)]
    # max ent should be able to be computed statically - if each question has the same number
    # of options m, then each question needs (1/m)(log(1/m)) * m = log m bits. If there are n 
    # questions, then we know there are n log m bits needed
    maxEnt = entropy(maxEntFlips)
    print(maxEnt, N*log(coinSides))
    # assert(maxEnt==N*log(coinSides)) gah, floating point!
    # List of normalized entropies, for sorting later to find confidence interval.
    # etosch : what are they supposed to be normalized over?
    entropies = []
    # Start the simulations.
    for kk in range(1, simulations+1):
        # Initialize the flips array, which counts the occurrences of
        # each "side". etosch : i.e. each option -> this is what is observed
        # are we aligning the options? are these supposed to be positions?
        flips  = [0 for x in range(0, coinSides)]
        # Flip N "coins". etosch : pick randomly for every option
        for i in range(1, N+1):
            v = randint(0, coinSides-1)
            # Add one to the appropriate bin.
            flips[v] += 1
        # Add the normalized entropy.
        # etosch : why are we computing the entropy over a single response? 
        # the flips array has to correspond to the surface text of a question - what's off to me
        # is the idea that we're combining the idea of "bias" in the question (i.e. that the
        # distribution has a mode) with the idea that there's bias across all questions. You get
        # no information from a single response. Instead, you need to know how one person 
        # response compares with the group. This just seems to flatten the survey and lose information
        # in a way that's analogous to what we saw with the maximum likelihood
        entropies.append ((flips, entropy(flips) / maxEnt))

    entropies.sort()
    print "%f%% of entropies greater than %f" % \
    (100 * confidence, entropies[int((1.0 - confidence) * simulations)][1])

    print entropies[int((1.0 - confidence) * simulations)][0]

simulate(N = 50, coinSides = 8, confidence = 0.90)


