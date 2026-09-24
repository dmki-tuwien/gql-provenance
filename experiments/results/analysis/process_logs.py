import re
import sys

arg1 = sys.argv[1]
INPUT_FILE = "result/phase_metrics/"+arg1
OUTPUT_FILE = "result/phase_metrics/processed/"+arg1

# Patterns
# prov (fine-grained): property = {ints}:{uuid}:{ints}.{text}  e.g. 4:uuid:604.id
PROP_PATTERN = re.compile(r'\d+:[0-9a-f-]+:\d+\.[a-zA-Z_][a-zA-Z0-9_]*$')
# prov (fine-grained): label = {ints}:{uuid}:{ints}:{text}  e.g. 5:uuid:56171:INVEST
LABEL_PATTERN = re.compile(r'\d+:[0-9a-f-]+:\d+:[A-Z_][A-Z0-9_]*$')
# prov_coarse: {ints}:{uuid}:{ints}.{#labels}.{#properties}  e.g. 4:uuid:13391.1.0
COARSE_PATTERN = re.compile(r'\d+:[0-9a-f-]+:\d+\.(\d+)\.(\d+)$')


def parse_prov_array(prov_str):
    """Parse the prov=[[...],[...]] structure into a list of witness arrays."""
    witnesses = []
    inner = re.findall(r'\[([^\[\]]*)\]', prov_str)
    for witness_str in inner:
        elements = [e.strip() for e in witness_str.split(',') if e.strip()]
        witnesses.append(elements)
    return witnesses


def count_fine_grained(elements):
    """Count properties and labels in a fine-grained witness."""
    props = sum(1 for e in elements if PROP_PATTERN.match(e.strip()))
    labels = sum(1 for e in elements if LABEL_PATTERN.match(e.strip()))
    return props, labels


def count_coarse(elements):
    """Sum up #labels and #properties from coarse witness elements."""
    total_labels = 0
    total_props = 0
    for e in elements:
        m = COARSE_PATTERN.match(e.strip())
        if m:
            total_labels += int(m.group(1))
            total_props += int(m.group(2))
    return total_labels, total_props


def parse_end_line(line):
    """Parse a log line with prov_phase=end.
    Schema: date | prov_model | dataset | scale_factor | query_tag | parameter
            | prov_phase | timestamp | num_results | actual_output
    The difference from the original format: actual_output is now a single object
        { result={...}, prov=[[...]], witnesses=N }
    rather than an array of such objects.
    """
    parts = line.split(' | ')
    if len(parts) < 9:
        return None

    date         = parts[0].strip()
    prov_model   = parts[1].strip()
    dataset      = parts[2].strip()
    scale_factor = parts[3].strip()
    query_tag    = parts[4].strip()
    parameter    = parts[5].strip()
    prov_phase   = parts[6].strip()
    timestamp    = parts[7].strip()
    actual_output = ' | '.join(parts[8:]).strip()

    rest, query_number = query_tag.split("-")
    query_type, _ = rest.rsplit("_",1)

    # Determine query type from tag
    # if not (query_type.startswith('prov_coarse') or query_type.startswith('prov')):
    #     return None  # orig, rewritten etc — skip

    if prov_phase != 'witness':
        # return None
        return {
            'date': date,
            'prov_model': prov_model,
            'dataset': dataset,
            'scale_factor': scale_factor,
            'query_number': query_number,
            'parameter': parameter,
            'query_type': query_type,
            'prov_phase': prov_phase,
            'timestamp': timestamp,
            'actual_output': ''
        }

    return {
        'date': date,
        'prov_model': prov_model,
        'dataset': dataset,
        'scale_factor': scale_factor,
        'query_number': query_number,
        'parameter': parameter,
        'query_type': query_type,
        'prov_phase': prov_phase,
        'timestamp': timestamp,
        'actual_output': actual_output
    }


def parse_single_result_block(actual_output):
    """Parse a single result block of the form
        { result={...}, prov=[[...],[...]], witnesses=N }
    (no surrounding array — each log line is already one result).
    """
    result_match = re.search(r'result=\{(.*?)\},\s*prov=', actual_output, re.DOTALL)
    result_str = result_match.group(1).strip() if result_match else ''

    witnesses_match = re.search(r'witnesses=(\d+)', actual_output)
    witnesses_count = int(witnesses_match.group(1)) if witnesses_match else 0

    prov_match = re.search(r'prov=(\[.*\]),\s*witnesses=', actual_output, re.DOTALL)
    prov_str = prov_match.group(1) if prov_match else '[]'

    witnesses = parse_prov_array(prov_str)

    return result_str, witnesses_count, witnesses


def process_file():
    rows = []
    header = [
        'date', 'prov_model', 'dataset', 'scale_factor', 'query_number', 'parameter', 'query_type', 'prov_phase',
        'timestamp', 'witness_index', 'witnesses_per_result',
        'annotation_count', 'property_count', 'label_count',
        'result_value', 'witness_elements', 'coarse_elements'
    ]

    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        for raw_line in f:
            line = raw_line.strip()

            parsed = parse_end_line(line)

            if parsed is None:
                continue

            query_type    = parsed['query_type']
            actual_output = parsed['actual_output']

            # Each line is already one result — parse the single block directly
            result_str, witnesses_count, witnesses = parse_single_result_block(actual_output)
            witnesses_per_result = len(witnesses)

            for wit_idx, witness_elements in enumerate(witnesses):
                annotation_count = len(witness_elements)

                prop_count = 0
                label_count = 0
                coarse_witness_str = ''

                if query_type == 'prov_result':
                    prop_count, label_count = count_fine_grained(witness_elements)
                    uri_elements = sorted([
                        x for x in witness_elements
                        if isinstance(x, str) and ":" in x and x.count(":") == 2 and "." not in x
                    ])
                    coarse_witness_str = ' , '.join(uri_elements)
                elif query_type == 'prov_coarse_result': # prov_coarse
                    label_count, prop_count = count_coarse(witness_elements)
                    uri_names = sorted([
                        x.split(".")[0]
                        for x in witness_elements
                        if isinstance(x, str)
                           and x.count(".") == 2
                           and x.split(".")[1].isdigit()
                           and x.split(".")[2].isdigit()
                    ])
                    coarse_witness_str = ' , '.join(uri_names)

                # Use pipe separator since commas appear inside element strings
                witness_str = ' , '.join(witness_elements)

                rows.append([
                    parsed['date'],
                    parsed['prov_model'],
                    parsed['dataset'],
                    parsed['scale_factor'],
                    parsed['query_number'],
                    parsed['parameter'],
                    query_type,
                    parsed['prov_phase'],
                    parsed['timestamp'],
                    wit_idx + 1,
                    witnesses_per_result,
                    annotation_count,
                    prop_count,
                    label_count,
                    result_str,
                    witness_str,
                    coarse_witness_str
                ])

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write('|'.join(header) + '\n')
        for row in rows:
            f.write('|'.join(str(x) for x in row) + '\n')

    print(f"Done. Written {len(rows)} rows to {OUTPUT_FILE}")
    return rows


rows = process_file()

# Print a sample
print("\nSample output (first 3 rows):")
print('\t'.join([
    'date', 'prov_model', 'dataset', 'scale_factor', 'query_number', 'parameter', 'query_type', 'prov_phase',
    'timestamp', 'witness_index', 'witnesses_per_result',
    'annotation_count', 'property_count', 'label_count',
    'result_value', 'witness_elements', 'coarse_elements'
]))
for r in rows[:3]:
    print('\t'.join(str(x) for x in r))
