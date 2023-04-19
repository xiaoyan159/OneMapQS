/*
 * Copyright 2017 nebular
 * Copyright 2017 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.navinfo.collect.library.map.cluster

import org.oscim.layers.marker.InternalItem

/**
 * Extension for clustered items.
 */
class Clustered : InternalItem() {
    /**
     * If this is >0, this item will be displayed as a cluster circle, with size clusterSize+1.
     */
    var clusterSize = 0

    /**
     * If this is true, this item is hidden (because it's represented by another InternalItem acting as cluster.
     */
    var clusteredOut = false
}