//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011-2013, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.flump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playn.core.Asserts;
import playn.core.Image;
import playn.core.Json;
import playn.core.util.Callback;
import static playn.core.PlayN.*;

import react.Value;

import tripleplay.util.TexturePacker;

public class Library
{
    /** The original frame rate of movies in this library. */
    public final float frameRate;

    /** The symbols defined in this library. */
    public final Map<String,Symbol> symbols;

    protected Library (Json.Object json, String baseDir, final Callback<Library> callback) {
        frameRate = json.getNumber("frameRate");

        final Map<String,Symbol> symbols = new HashMap<String,Symbol>();
        this.symbols = Collections.unmodifiableMap(symbols);

        final List<Movie.Symbol> movies = new ArrayList<Movie.Symbol>();
        for (Json.Object movieJson : json.getArray("movies", Json.Object.class)) {
            Movie.Symbol movie = new Movie.Symbol(this, movieJson);
            movies.add(movie);
            symbols.put(movie.name(), movie);
        }

        Json.TypedArray<Json.Object> textureGroups =
            json.getArray("textureGroups", Json.Object.class);
        // TODO(bruno): Support multiple scaleFactors?
        Json.TypedArray<Json.Object> atlases =
            textureGroups.get(0).getArray("atlases", Json.Object.class);
        final Value<Integer> remainingAtlases = Value.create(atlases.length());

        remainingAtlases.connectNotify(new Value.Listener<Integer>() {
            @Override public void onChange (Integer remaining, Integer _) {
                if (remaining > 0) return;

                // When all the symbols have been loaded, go through and resolve references
                for (Movie.Symbol movie : movies) {
                    for (LayerData layer : movie.layers) {
                        for (KeyframeData kf : layer.keyframes) {
                            if (kf._symbolName != null) {
                                Symbol symbol = symbols.get(kf._symbolName);
                                Asserts.checkNotNull(symbol);

                                if (layer._lastSymbol == null) layer._lastSymbol = symbol;
                                else if (layer._lastSymbol != symbol) layer._multipleSymbols = true;
                                kf._symbol = symbol;
                            }
                        }
                    }
                }

                // We're done!
                callback.onSuccess(Library.this);
            }
        });

        for (final Json.Object atlasJson : atlases) {
            Image atlas = assets().getImage(baseDir + "/" + atlasJson.getString("file"));
            atlas.addCallback(new Callback.Chain<Image>(callback) {
                public void onSuccess (Image atlas) {
                    for (Json.Object textureJson : atlasJson.getArray("textures", Json.Object.class)) {
                        Texture.Symbol texture = new Texture.Symbol(textureJson, atlas);
                        symbols.put(texture.name(), texture);
                    }
                    remainingAtlases.update(remainingAtlases.get() - 1);
                }
            });
        }
    }

    /**
     * Loads a Library from PlayN assets.
     * @param baseDir The base directory, containing library.json and texture atlases.
     */
    public static void fromAssets (final String baseDir, final Callback<Library> callback) {
        Asserts.checkNotNull(callback);
        assets().getText(baseDir + "/library.json", new Callback.Chain<String>(callback) {
            public void onSuccess (String text) {
                try {
                    new Library(json().parse(text), baseDir, callback);
                } catch (Exception err) {
                    callback.onFailure(err);
                }
            }
        });
    }

    /** Pack multiple libraries into a single group of atlases. The libraries will be modified so
     * that their symbols point at the new atlases. */
    public static void pack (Collection<Library> libs) {
        List<Library> list = new ArrayList<Library>(libs);

        // Add all texture symbols to the packer
        TexturePacker packer = new TexturePacker();
        for (int ii = 0, ll = list.size(); ii < ll; ++ii) {
            Library lib = list.get(ii);
            for (Symbol symbol : lib.symbols.values()) {
                if (symbol instanceof Texture.Symbol) {
                    packer.add(ii+":"+symbol.name(), ((Texture.Symbol)symbol).region);
                }
            }
        }

        // Pack and update all texture symbols to the new regions
        Map<String,Image.Region> images = packer.pack();
        for (int ii = 0, ll = list.size(); ii < ll; ++ii) {
            Library lib = list.get(ii);
            for (Symbol symbol : lib.symbols.values()) {
                if (symbol instanceof Texture.Symbol) {
                    ((Texture.Symbol)symbol).region = images.get(ii+":"+symbol.name());
                }
            }
        }
    }

    /** Creates an instance of a symbol, or throws if the symbol name is not in this library. */
    public Instance createInstance (String symbolName) {
        Symbol symbol = symbols.get(symbolName);
        if (symbol == null) {
            throw new IllegalArgumentException("Missing required symbol [name=" + symbolName + "]");
        }
        return symbol.createInstance();
    }

    public Movie createMovie (String symbolName) {
        return (Movie)createInstance(symbolName);
    }

    public Texture createTexture (String symbolName) {
        return (Texture)createInstance(symbolName);
    }
}
