/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package apps;

import java.io.InputStream;
import java.net.URL;

public class Resources {

    private Resources() {}

    public static URL getUrl(String name) {
        return  Resources.class.getResource(name);
    }

    public static InputStream getStream(String name) {
        return Resources.class.getClassLoader().getResourceAsStream(name);
    }

    public static String loadResource(String name) {
        return Resources.class.getClassLoader().getResource(name).toExternalForm();
    }
}
