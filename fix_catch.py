import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # First, let's normalize existing ones to avoid duplicates
    content = re.sub(r'catch\s*\(\s*e\s*:\s*Exception\s*\)\s*\{\s*if\s*\(\s*e\s*is\s*kotlinx\.coroutines\.CancellationException\s*\)\s*throw\s*e\s*;?', r'catch (e: Exception) {', content)
    
    # Now add it to all catch (e: Exception) {
    content = re.sub(r'catch\s*\(\s*e\s*:\s*Exception\s*\)\s*\{', r'catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

for root, dirs, files in os.walk(r'd:\Documents\KapwaDVOv2\app\src\main\java'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
print('Done processing catch blocks.')
