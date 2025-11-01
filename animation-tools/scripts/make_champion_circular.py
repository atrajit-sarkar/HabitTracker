"""
Make champion.png circular with a border
"""
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np

def make_circular(input_path, output_path, border_width=8, border_color=(255, 215, 0, 255)):
    """
    Make image circular with a gold border
    
    Args:
        input_path: Path to input PNG
        output_path: Path to output PNG
        border_width: Width of the border in pixels
        border_color: RGBA color of the border (default: gold)
    """
    print(f"🎨 Making image circular: {input_path}")
    
    # Open image
    img = Image.open(input_path).convert('RGBA')
    
    # Create a square image (use the smaller dimension)
    width, height = img.size
    size = min(width, height)
    
    # Crop to square from center
    left = (width - size) // 2
    top = (height - size) // 2
    img_square = img.crop((left, top, left + size, top + size))
    
    # Create output image with transparency
    output_size = size
    output = Image.new('RGBA', (output_size, output_size), (0, 0, 0, 0))
    
    # Create circular mask
    mask = Image.new('L', (output_size, output_size), 0)
    draw = ImageDraw.Draw(mask)
    
    # Draw filled circle for the image
    draw.ellipse((0, 0, output_size, output_size), fill=255)
    
    # Apply mask to image
    output.paste(img_square, (0, 0))
    output.putalpha(mask)
    
    # Create border by drawing a circle on top
    if border_width > 0:
        draw_output = ImageDraw.Draw(output)
        # Draw outer circle (border)
        for i in range(border_width):
            draw_output.ellipse(
                (i, i, output_size - 1 - i, output_size - 1 - i),
                outline=border_color
            )
    
    # Save result
    output.save(output_path, 'PNG', optimize=True)
    print(f"✅ Saved circular image: {output_path}")
    print(f"🎨 Gold border added!")

def process_champion_circular():
    """Process champion.png to make it circular"""
    script_dir = Path(__file__).parent.parent.parent
    input_dir = script_dir / 'widgets-assets'
    output_dir = script_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    input_path = input_dir / 'champion.png'
    output_path = output_dir / 'widget_champion.png'
    
    if input_path.exists():
        make_circular(str(input_path), str(output_path), border_width=10, border_color=(255, 215, 0, 255))
        print(f"\n🏆 Circular champion image ready!")
        print(f"📱 Use in widget as: R.drawable.widget_champion")
    else:
        print(f"❌ File not found: {input_path}")

if __name__ == '__main__':
    print("🔧 Making champion.png circular with gold border...\n")
    process_champion_circular()
