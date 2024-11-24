package sekelsta.horse_colors.genetics;


import net.minecraft.entity.EntityAgeable;
import sekelsta.horse_colors.genetics.breed.Breed;

import java.util.List;
import java.util.Random;

public class FakeGeneticEntity implements IGeneticEntity {
    private EquineGenome genome;
    private String geneData;
    private boolean gender;
    private float motherSize;
    private int seed;

    public FakeGeneticEntity() {
        geneData = "";
    }

    @Override
    public EquineGenome getGenome() {
        return genome;
    }

    @Override
    public String getGeneData() {
        return geneData;
    }

    @Override
    public void setGeneData(String genes) {
        this.geneData = genes;
    }

    @Override
    public int getSeed() {
        return seed;
    }

    @Override
    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public Random getRand() {
        return new Random();
    }

    @Override
    public boolean isMale() {
        return gender;
    }

    @Override
    public void setMale(boolean gender) {
        this.gender = gender;
    }

    @Override
    public boolean isFertile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFertile(boolean fertile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRebreedTicks() {
        return 0;
    }

    @Override
    public int getBirthAge() {
        return 0;
    }

    @Override
    public int getTrueAge() {
        return 0;
    }

    @Override
    public boolean setPregnantWith(EntityAgeable child, EntityAgeable otherParent) {
        return false;
    }

    @Override
    public float getMotherSize() {
        return this.motherSize;
    }

    @Override
    public void setMotherSize(float size) {
        this.motherSize = size;
    }

    @Override
    public List<Breed> getBreeds() {
        return null;
    }
}
