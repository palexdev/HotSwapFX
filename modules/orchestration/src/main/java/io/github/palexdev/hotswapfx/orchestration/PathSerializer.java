package io.github.palexdev.hotswapfx.orchestration;

import java.nio.file.Path;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

class PathSerializer extends FieldSerializer<Path> {

    //================================================================================
    // Constructors
    //================================================================================

    public PathSerializer(Kryo kryo, Class type) {
        super(kryo, type);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void write(Kryo kryo, Output output, Path path) {
        kryo.writeObject(output, path.toString());
    }

    @Override
    public Path read(Kryo kryo, Input input, Class<? extends Path> type) {
        return Path.of(kryo.readObject(input, String.class));
    }
}
