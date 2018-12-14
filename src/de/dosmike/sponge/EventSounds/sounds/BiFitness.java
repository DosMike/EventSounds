package de.dosmike.sponge.EventSounds.sounds;

/**
 * This is a rating test function, as it could be used to evaluate a neural network output.
 * It will take two objects of type L and R and return a Numeric fitness for it's inputs.
 */
@FunctionalInterface
public interface BiFitness<Z extends Number,L,R> {
    Z test(L l, R r);
}
