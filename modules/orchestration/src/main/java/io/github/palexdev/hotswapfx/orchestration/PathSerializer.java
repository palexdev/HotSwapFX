/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of HotSwapFX (https://github.com/palexdev/HotSwapFX)
 *
 * HotSwapFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * HotSwapFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HotSwapFX. If not, see <http://www.gnu.org/licenses/>.
 */

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
