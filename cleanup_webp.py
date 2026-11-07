import os

res_dir = 'app/src/main/res'
densities = ['mipmap-mdpi', 'mipmap-hdpi', 'mipmap-xhdpi', 'mipmap-xxhdpi', 'mipmap-xxxhdpi']

for density in densities:
    folder = os.path.join(res_dir, density)
    if os.path.exists(folder):
        for f in os.listdir(folder):
            if f.endswith('.webp'):
                path = os.path.join(folder, f)
                os.remove(path)
                print(f"Removed old webp: {path}")

print("Cleanup of old webp launcher icons completed.")
