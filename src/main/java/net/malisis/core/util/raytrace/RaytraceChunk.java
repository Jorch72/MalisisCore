/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.core.util.raytrace;

import java.util.Optional;
import java.util.Set;

import net.malisis.core.MalisisCore;
import net.malisis.core.util.Point;
import net.malisis.core.util.Ray;
import net.malisis.core.util.Utils;
import net.malisis.core.util.Vector;
import net.malisis.core.util.chunkcollision.ChunkCollision;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.google.common.collect.Sets;

/**
 * @author Ordinastie
 *
 */
public class RaytraceChunk extends Raytrace
{
	private static final int MAX_CHUNKS = 16;
	/** World object (needed for ray tracing inside each block). */
	private World world;
	/** Vector describing the direction of steps to take when reaching limits of a block. */
	private Vector step;

	private Set<Chunk> chunks = Sets.newHashSet();

	/**
	 * Instantiates a new {@link RaytraceChunk}.
	 *
	 * @param ray the ray
	 * @param options the options
	 */
	public RaytraceChunk(World world, Ray ray)
	{
		super(ray);
		this.world = world;

		int stepX = 1, stepZ = 1;
		if (ray.direction.x < 0)
			stepX = -1;
		if (ray.direction.z < 0)
			stepZ = -1;

		step = new Vector(stepX, 0, stepZ);
	}

	/**
	 * Instantiates a new {@link RaytraceChunk}.
	 *
	 * @param src the src
	 * @param v the v
	 * @param options the options
	 */
	public RaytraceChunk(World world, Point src, Vector v)
	{
		this(world, new Ray(src, v));
	}

	/**
	 * Instantiates a new {@link RaytraceChunk}.
	 *
	 * @param src the src
	 * @param dest the dest
	 * @param options the options
	 */
	public RaytraceChunk(World world, Point src, Point dest)
	{
		this(world, new Ray(src, new Vector(src, dest)));
		this.dest = dest;
	}

	/**
	 * Does the raytracing.
	 *
	 * @return {@link RayTraceResult} with <code>typeOfHit</code> <b>BLOCK</b> if a ray hits a block in the way, or <b>MISS</b> if it
	 *         reaches <code>dest</code> without any hit
	 */
	public RayTraceResult trace()
	{
		RayTraceResult mop = null, firstHit = null;
		double tX, tZ, min;
		int count = 0;
		boolean ret = false;

		int currentX = ((int) src.x) >> 4;
		int currentZ = ((int) src.y) >> 4;

		while (!ret && count++ <= MAX_CHUNKS)
		{
			tX = ray.intersectX(currentX + (ray.direction.x > 0 ? 1 : 0));
			tZ = ray.intersectZ(currentZ + (ray.direction.z > 0 ? 1 : 0));

			min = getMin(tX, tZ);
			Point exit = ray.getPointAt(min);
			if (exit == null || exit.y <= 0 || exit.y >= 256)
				ret = true;

			if (dest != null && Point.distanceSquared(src, dest) < Point.distanceSquared(src, exit))
				ret = true;

			Optional<Chunk> chunk = Utils.getLoadedChunk(world, new BlockPos(currentX << 4, 0, currentZ << 4));
			if (chunk.isPresent())
			{
				mop = ChunkCollision.get().processCallbacks(chunk.get());
				chunks.add(chunk.get());
			}
			else
				ret = true;

			firstHit = Raytrace.getClosestHit(Type.BLOCK, src, firstHit, mop);

			if (dest != null && currentX == ((int) dest.x) << 4 && currentZ == ((int) dest.y) << 4)
				ret = true;

			if (!ret)
			{
				if (min == tX)
					currentX += step.x;
				if (min == tZ)
					currentZ += step.z;
			}

			if (dest != null && dest.equals(ray.getPointAt(min)))
				ret = true;

		}

		if (firstHit == null && dest != null)
			firstHit = new RayTraceResult(RayTraceResult.Type.MISS, dest.toVec3d(), null, new BlockPos(currentX, 0, currentZ));

		if (!ret)
			MalisisCore.message("Trace fail : " + MAX_CHUNKS + " chunks passed (" + currentX + "," + currentZ + ")");
		return firstHit;
	}

	/**
	 * Gets the minimum value of <code>x</code>, <code>z</code>.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return <code>Double.NaN</code> if <code>x</code> and <code>z</code> are all three <code>Double.NaN</code>
	 */
	public double getMin(double x, double z)
	{
		double ret = Double.NaN;
		if (!Double.isNaN(x))
			ret = x;
		if (!Double.isNaN(z))
		{
			if (!Double.isNaN(ret))
				ret = Math.min(ret, z);
			else
				ret = z;
		}
		return ret;
	}
}
