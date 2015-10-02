package cs.bilkent.zanza.operator;

import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Used for incoming and outgoing tuples
 */
public class PortsToTuples
{

	private final Int2ObjectHashMap<List<Tuple>> tuplesByPort = new Int2ObjectHashMap<>();

	public PortsToTuples()
	{
	}

	public PortsToTuples(Tuple tuple)
	{
		add(tuple);
	}

	public PortsToTuples(List<Tuple> tuples)
	{
		addAll(tuples);
	}

	public void add(Tuple tuple)
	{
		add(Port.DEFAULT_PORT_INDEX, tuple);
	}

	public void addAll(List<Tuple> tuples)
	{

		addAll(Port.DEFAULT_PORT_INDEX, tuples);
	}

	public void add(int portIndex, Tuple tuple)
	{
		final List<Tuple> tuples = getOrCreateTuples(portIndex);
		tuples.add(tuple);
	}

	public void addAll(int portIndex, List<Tuple> tuplesToAdd)
	{
		final List<Tuple> tuples = getOrCreateTuples(portIndex);
		tuples.addAll(tuplesToAdd);
	}

	private List<Tuple> getOrCreateTuples(int portIndex)
	{
		return tuplesByPort.computeIfAbsent(portIndex, ignoredPortIndex -> new ArrayList<>());
	}

	public List<Tuple> getTuples(int portIndex)
	{
		final List<Tuple> tuples = tuplesByPort.get(portIndex);
		return tuples != null ? tuples : Collections.emptyList();
	}

	public List<Tuple> getTuplesByDefaultPort()
	{
		return getTuples(Port.DEFAULT_PORT_INDEX);
	}

	public int[] getPorts()
	{
		final Int2ObjectHashMap<List<Tuple>>.KeySet keys = tuplesByPort.keySet();
		final int[] ports = new int[keys.size()];

		final Int2ObjectHashMap<List<Tuple>>.KeyIterator it = keys.iterator();

		for (int i = 0; i < keys.size(); i++)
		{
			ports[i] = it.nextInt();
		}

		Arrays.sort(ports);

		return ports;
	}

	public int getPortCount()
	{
		return tuplesByPort.keySet().size();
	}

}
