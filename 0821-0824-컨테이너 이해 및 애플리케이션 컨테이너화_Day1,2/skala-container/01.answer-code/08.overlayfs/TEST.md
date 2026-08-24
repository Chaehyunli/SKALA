mkdir -p /mnt/ovtest

mount -t tmpfs tmpfs /mnt/ovtest

mkdir -p /mnt/ovtest/{lower,upper,work,merged}

echo "AAA from lower" > /mnt/ovtest/lower/a.txt
echo "BBB from lower" > /mnt/ovtest/lower/b.txt

mount -t overlay overlay \
  -o lowerdir=/mnt/ovtest/lower,upperdir=/mnt/ovtest/upper,workdir=/mnt/ovtest/work \
  /mnt/ovtest/merged


mountpoint /mnt/ovtest/merged

ls -la /mnt/ovtest/merged

