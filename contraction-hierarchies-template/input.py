from typing import List, Tuple

import numpy as np


def input_generator(seed: int, n: int = 1000) -> List[Tuple[int, int]]:
    """Generate ```n``` random s,t pairs from denmark.graph. Right now we allow duplicates. Maybe we should, maybe not"""
    
    # 115724 to 5423454068

    rng = np.random.default_rng(seed) #returns a random number generator using the seed from the arguments
    values = rng.integers(115724, 5423454068, size=(n,2), endpoint=True)
    return [tuple(pair) for pair in values.tolist()] #return the numbers in a list


if __name__ == "__main__":
    """Run experiment here"""