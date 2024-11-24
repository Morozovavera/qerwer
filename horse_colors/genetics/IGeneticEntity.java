package sekelsta.horse_colors.genetics;


import net.minecraft.entity.EntityAgeable;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.genetics.breed.Breed;

import java.util.Collection;
import java.util.List;
import java.util.Random;


public interface IGeneticEntity<T extends Enum<T>> {
    EquineGenome getGenome();

    String getGeneData();
    void setGeneData(String genes);

    int getSeed();
    void setSeed(int seed);

    Random getRand();

    boolean isMale();
    void setMale(boolean gender);

    boolean isFertile();
    void setFertile(boolean fertile);

    int getRebreedTicks();

    int getBirthAge();
    int getTrueAge();

    default float getFractionGrown() {
        int age = getTrueAge();
        if (age < 0) {
            if (HorseConfig.GROWTH.growGradually) {
                int minAge = getBirthAge();
                float fractionGrown = (minAge - age) / (float)minAge;
                return Math.max(0, fractionGrown);
            }
            return 0;
        }
        return 1;
    }

    // Return true if successful, false otherwise
    // Reasons for returning false could be if the animal is male or the mate is female
    // (This prevents spawn eggs from starting a pregnancy.)
    boolean setPregnantWith(EntityAgeable child, EntityAgeable otherParent);

    default Breed<T> getDefaultBreed() {
        return new Breed<>();
    }

    default int getPopulation() {
        int count = 0;
        for (Breed<T> breed : getBreeds()) {
            count += breed.population;
        }
        return count;
    }

    Collection<Breed<T>> getBreeds();

    default Breed<T> getRandomBreed() {
        int r = getRand().nextInt(Math.max(1, getPopulation()));
        int count = 0;
        for (Breed<T> breed : getBreeds()) {
            count += breed.population;
            if (r < count) {
                return breed;
            }
        }
        return getDefaultBreed();
    }

    default Breed<T> getBreed(String name) {
        for (Breed<T> breed : getBreeds()) {
            if (name.equals(breed.name)) {
                return breed;
            }
        }
        return null;
    }

    float getMotherSize();
    void setMotherSize(float size);
}
