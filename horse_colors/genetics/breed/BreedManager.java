package sekelsta.horse_colors.genetics.breed;

import com.google.common.collect.Maps;
import com.google.gson.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.genetics.EquineGenome;
import sekelsta.horse_colors.genetics.breed.horse.Tarpan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BreedManager   {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<String, Breed<EquineGenome.Gene>> breeds = new HashMap<>();

    public static final List<Breed<EquineGenome.Gene>> horses = new ArrayList<>();
    public static final List<Breed<EquineGenome.Gene>> donkeys = new ArrayList<>();

    public BreedManager() {

        loadBreed(true, "appaloosa");
        loadBreed(true,"cleveland_bay");
        loadBreed(true,"default_horse");
        loadBreed(true,"fjord");
        loadBreed(true,"friesian");
        loadBreed(true,"hucul");
        loadBreed(true,"mongolian_horse");
        loadBreed(true,"quarter_horse");

        loadBreed(false,"default_donkey");
        loadBreed(false,"large_donkey");
        loadBreed(false,"miniature_donkey");

        postProcess();
    }

    public void loadBreed(boolean isHorse, String name) {

        for(String key : mapIn(name).keySet()) {
            try {
                // Possible IllegalStateException will be caught
                JsonObject json = mapIn(name).get(key).getAsJsonObject();
                Breed<EquineGenome.Gene> b = deserializeBreed(json, name);
                breeds.put(key, b);
                if(isHorse)horses.add(b);
                else donkeys.add(b);
            }
            catch (IllegalStateException e) {
                HorseColors.logger.error("Could not parse json: " + key);
            }
            catch (ClassCastException e) {
                HorseColors.logger.error("Unexpected data type in json: " + key);
            }
            HorseColors.logger.debug("Loaded " + breeds.size()
                    + " breed data files");
        }
    }


    protected Map<String, JsonElement> mapIn(String name) {
        Map<String, JsonElement> map = Maps.newHashMap();
        try {
            Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

            try {
                InputStream inputstream = HorseColors.class.getResourceAsStream("/assets/horse_colors/breed/" + name +".json");
                Throwable var12 = null;

                try {
                    Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
                    Throwable var14 = null;

                    try {
                        JsonElement jsonelement = JsonUtils.fromJson(GSON, reader, JsonElement.class);
                        if (jsonelement != null) {
                            JsonElement jsonelement1 = map.put(name, jsonelement);
                            if (jsonelement1 != null) {
                                throw new IllegalStateException("Duplicate data file ignored with ID " + name);
                            }
                        } else {
                            LOGGER.error("Couldn't load data file {} from {} as it's null or empty", name, name);
                        }
                    } catch (Throwable var62) {
                        var14 = var62;
                        throw var62;
                    } finally {
                        if (reader != null) {
                            if (var14 != null) {
                                try {
                                    reader.close();
                                } catch (Throwable var61) {
                                    var14.addSuppressed(var61);
                                }
                            } else {
                                reader.close();
                            }
                        }

                    }
                } catch (Throwable var64) {
                    var12 = var64;
                    throw var64;
                } finally {
                    if (inputstream != null) {
                        if (var12 != null) {
                            try {
                                inputstream.close();
                            } catch (Throwable var60) {
                                var12.addSuppressed(var60);
                            }
                        } else {
                            inputstream.close();
                        }
                    }

                }
            } catch (Throwable var66) {
                throw var66;
            }
        } catch (IOException | JsonParseException | IllegalArgumentException var68) {
            LOGGER.error("Couldn't parse data file from", var68);
        }

        return map;
    }

    private static Breed<EquineGenome.Gene> deserializeBreed(JsonObject json, String name)
            throws ClassCastException, IllegalStateException
    {
        Breed<EquineGenome.Gene> breed = new Breed<>(EquineGenome.Gene.class);
        breed.name = name;
        if (json.has("genes")) {
            JsonObject genesJson = (JsonObject)json.get("genes");
            for (Map.Entry<String, JsonElement> entry : genesJson.entrySet()) {
                JsonArray array = entry.getValue().getAsJsonArray();
                ArrayList<Float> frequencies = new ArrayList<>();
                for (int i = 0; i < array.size(); ++i) {
                    frequencies.add(array.get(i).getAsFloat());
                }
                breed.genes.put(EquineGenome.Gene.valueOf(entry.getKey()), frequencies);
            }
        }
        if (json.has("population")) {
            breed.population = json.get("population").getAsInt();
        }
        return breed;
    }


    private void postProcess() {
        // TODO: find a less ugly way
        Breed<EquineGenome.Gene> mongolianHorse = getBreed("mongolian_horse");
        mongolianHorse.parent = Tarpan.breed;

        Breed<EquineGenome.Gene> hucul = getBreed("hucul");
        hucul.parent = mongolianHorse;

        Breed<EquineGenome.Gene> quarterHorse = getBreed("quarter_horse");
        quarterHorse.parent = mongolianHorse;

        Breed<EquineGenome.Gene> defaultHorse = getBreed("default_horse");
        defaultHorse.parent = mongolianHorse;

        Breed<EquineGenome.Gene> clevelandBay = getBreed("cleveland_bay");
        clevelandBay.parent = quarterHorse;

        Breed<EquineGenome.Gene> appaloosa = getBreed("appaloosa");
        appaloosa.parent = quarterHorse;

        Breed<EquineGenome.Gene> fjord = getBreed("fjord");
        fjord.parent = quarterHorse;

        Breed<EquineGenome.Gene> friesian = getBreed("friesian");
        friesian.parent = mongolianHorse;

        Breed<EquineGenome.Gene> defaultDonkey = getBreed("default_donkey");
        defaultDonkey.parent = BaseDonkey.breed;

        Breed<EquineGenome.Gene> large = getBreed("large_donkey");
        large.parent = defaultDonkey;

        Breed<EquineGenome.Gene> mini = getBreed("miniature_donkey");
        mini.parent = defaultDonkey;

    }

    public static Breed<EquineGenome.Gene> getBreed(String name) {
        return breeds.get(name);
    }

}
