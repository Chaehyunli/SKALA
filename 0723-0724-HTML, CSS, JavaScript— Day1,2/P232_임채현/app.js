// HTML 요소 가져오기
const carForm = document.querySelector('#carForm')
const makerInput = document.querySelector('#makerInput')
const modelInput = document.querySelector('#modelInput')
const yearInput = document.querySelector('#yearInput')
const mileageInput = document.querySelector('#mileageInput')
const priceInput = document.querySelector('#priceInput')
const fuelInput = document.querySelector('#fuelInput')
const statusInput = document.querySelector('#statusInput')

const submitButton = document.querySelector('#submitButton')
const cancelEditButton = document.querySelector('#cancelEditButton')
const searchInput = document.querySelector('#searchInput')
const statusFilter = document.querySelector('#statusFilter')

const countText = document.querySelector('#countText')
const emptyMessage = document.querySelector('#emptyMessage')
const carList = document.querySelector('#carList')

// 차량 목록 배열이다. 차량 1대는 객체 1개로 저장한다.
let cars = [
  {
    id: 1,
    maker: '현대',
    model: '쏘나타',
    year: 2021,
    mileage: 43000,
    price: 1850,
    fuel: 'LPG',
    status: '판매중',
  },
  {
    id: 2,
    maker: '기아',
    model: 'K5',
    year: 2020,
    mileage: 52000,
    price: 1690,
    fuel: '가솔린',
    status: '예약중',
  },
]

// null이면 등록 모드, 숫자가 들어 있으면 수정 모드이다.
let editingId = null

// 처음 화면을 열었을 때 차량 목록을 출력한다.
renderCars()

// 차량 목록을 출력
function renderCars() {
  const filteredCars = getFilteredCars()

  // 기존 목록을 비운 뒤 현재 데이터 기준으로 다시 출력한다.
  carList.innerHTML = ''

  emptyMessage.hidden = filteredCars.length > 0
  countText.textContent = `전체 ${cars.length}대 / 표시 ${filteredCars.length}대`

  filteredCars.forEach(function (car) {
    const card = createCarCard(car)
    carList.appendChild(card)
  })
}
// 차량 목록을 필터링
function getFilteredCars() {
  const keyword = searchInput.value.trim().toLowerCase()
  const selectedStatus = statusFilter.value

  return cars.filter(function (car) {
    const searchText = `${car.maker} ${car.model}`.toLowerCase()
    const matchKeyword = searchText.includes(keyword)
    const matchStatus =
      selectedStatus === '전체' || car.status === selectedStatus

    return matchKeyword && matchStatus
  })
}
// 차량 카드 생성
function createCarCard(car) {
  const card = document.createElement('article')
  card.className = 'car-card'

  const title = document.createElement('h3')
  title.textContent = `${car.maker} ${car.model}`

  const info = document.createElement('p')
  info.textContent = `${car.year}년식 · ${car.fuel} · ${car.mileage.toLocaleString()}km`

  const price = document.createElement('p')
  price.className = 'price'
  price.textContent = `${car.price.toLocaleString()}만원`

  const status = document.createElement('span')
  status.className = `status-badge ${getStatusClass(car.status)}`
  status.textContent = car.status

  const actions = document.createElement('div')
  actions.className = 'card-actions'

  const editButton = document.createElement('button')
  editButton.type = 'button'
  editButton.dataset.action = 'edit'
  editButton.dataset.id = car.id
  editButton.textContent = '수정'

  const deleteButton = document.createElement('button')
  deleteButton.type = 'button'
  deleteButton.className = 'delete-button'
  deleteButton.dataset.action = 'delete'
  deleteButton.dataset.id = car.id
  deleteButton.textContent = '삭제'

  actions.appendChild(editButton)
  actions.appendChild(deleteButton)

  card.appendChild(title)
  card.appendChild(info)
  card.appendChild(price)
  card.appendChild(status)
  card.appendChild(actions)

  return card
}

// 입력 폼을 리셋
function resetForm() {
  editingId = null
  carForm.reset()
  submitButton.textContent = '등록'
  cancelEditButton.hidden = true
  modelInput.focus()
}

// 차량 상태 정보 설정
function getStatusClass(status) {
  if (status === '판매중') {
    return 'selling'
  }

  if (status === '예약중') {
    return 'reserved'
  }

  return 'sold'
}

// 차량 등록 또는 수정 처리
carForm.addEventListener('submit', function (event) {
  // TODO: [차량 등록 / 수정] 수강생 구현 #1
  // 폼 기본 제출을 막고, 검증을 통과하면 등록/수정 모드에 맞게 처리.
  event.preventDefault()

  const car = getCarFromForm()
  if (!car) {
    return
  }

  if (editingId === null) {
    // 등록 모드: 기존 id 중 최댓값 + 1을 새 id로 부여함
    car.id = cars.length > 0 ? Math.max(...cars.map((c) => c.id)) + 1 : 1
    cars.push(car)
  } else {
    // 수정 모드: 같은 id를 가진 차량만 새 값으로 교체함
    car.id = editingId
    cars = cars.map((c) => (c.id === editingId ? car : c))
  }

  resetForm() // 등록/수정 후 입력폼 초기화함
  renderCars() // 변경된 목록 다시 그림
})

// 차량 카드 안의 수정, 삭제 버튼 처리
carList.addEventListener('click', function (event) {
  // TODO: [차량 카드 안의 수정 / 삭제 버튼 처리] 수강생 구현 #2
  // 클릭한 버튼의 data-action, data-id로 수정/삭제를 구분해 처리.
  const button = event.target.closest('button') // 클릭한 지점에서 가장 가까운 button 요소 찾음
  if (!button) {
    return // 버튼이 아닌 곳을 클릭하면 무시함
  }

  const id = Number(button.dataset.id) // data-id 속성값을 숫자로 변환함

  if (button.dataset.action === 'edit') {
    startEdit(id) // 수정 버튼이면 수정 모드로 전환함
  } else if (button.dataset.action === 'delete') {
    deleteCar(id) // 삭제 버튼이면 삭제 처리함
  }
})

// 검색어를 입력할 때마다 목록을 다시 그림.
searchInput.addEventListener('input', renderCars) // 입력할 때마다 필터링해서 재출력함

// 판매 상태를 바꿀 때마다 목록을 다시 그림.
statusFilter.addEventListener('change', renderCars) // 상태 값이 바뀌면 필터링해서 재출력함

// 수정 취소
cancelEditButton.addEventListener('click', function () {
  // TODO: [수정 취소] 수강생 구현 #5
  // 수정 모드를 해제하고 입력 폼을 초기화.
  resetForm() // editingId를 null로 되돌리고 폼을 비움
})

// 입력폼에서 차량 정보 가져와서 객체로 반환
function getCarFromForm() {
  const maker = makerInput.value
  const model = modelInput.value.trim()
  const yearText = yearInput.value.trim()
  const mileageText = mileageInput.value.trim()
  const priceText = priceInput.value.trim()
  const fuel = fuelInput.value
  const status = statusInput.value

  const year = Number(yearText) // 문자열로 들어온 연식을 숫자로 변환함
  const mileage = Number(mileageText) // 주행거리도 숫자로 변환함
  const price = Number(priceText) // 가격도 숫자로 변환함
  const maxYear = new Date().getFullYear() // 연식 검증 상한값으로 올해 연도 사용함

  //TODO: [입력 정보 검증 및 차량 객체 반환] 수강생 구현 #6
  // 필수 항목 누락과 값 범위를 검증하고, 통과하면 차량 객체를 반환.
  if (!maker) {
    alert('제조사를 선택하세요.') // 제조사 미선택 시 경고함
    return null
  }

  if (!model) {
    alert('모델명을 입력하세요.') // 모델명 미입력 시 경고함
    return null
  }

  if (!yearText || year < 1990 || year > maxYear) {
    alert(`연식은 1990년부터 ${maxYear}년(현재 년도) 사이로 입력하세요.`) // 연식 범위를 벗어나면 경고함
    return null
  }

  if (!mileageText || mileage < 0) {
    alert('주행거리는 0 이상 입력하세요.') // 주행거리가 음수면 경고함
    return null
  }

  if (!priceText || price < 1) {
    alert('가격은 1이상 입력하세요.') // 가격이 1 미만이면 경고함
    return null
  }

  if (!fuel) {
    alert('연료를 선택하세요.') // 연료 미선택 시 경고함
    return null
  }

  return { maker, model, year, mileage, price, fuel, status } // 검증 통과 시 차량 객체 반환함
}

// 차량 정보 수정 설정
function startEdit(id) {
  //TODO: [차량 정보 수정 설정] 수강생 구현 #7
  // 선택한 차량 정보를 입력 폼에 채우고 수정 모드로 전환.
  const car = cars.find((c) => c.id === id) // id로 수정할 차량을 찾음
  if (!car) {
    return
  }

  editingId = id // 수정 모드로 전환하며 대상 id 기억함

  // 찾은 차량 정보를 입력 폼 각 항목에 채움
  makerInput.value = car.maker
  modelInput.value = car.model
  yearInput.value = car.year
  mileageInput.value = car.mileage
  priceInput.value = car.price
  fuelInput.value = car.fuel
  statusInput.value = car.status

  submitButton.textContent = '수정 완료' // 등록 버튼을 수정 완료 버튼으로 바꿈
  cancelEditButton.hidden = false // 수정 취소 버튼을 보이게 함

  modelInput.focus()
}

// 차량 정보 삭제
function deleteCar(id) {
  // TODO: [차량 정보 삭제] 수강생 구현 #8
  // 확인창을 띄운 뒤 확인하면 목록에서 제거하고 다시 그림.
  const confirmed = confirm('선택한 차량을 삭제할까요?') // 삭제 여부 확인창 띄움
  if (!confirmed) {
    return // 취소하면 아무 것도 안 함
  }

  cars = cars.filter((c) => c.id !== id) // 해당 id만 목록에서 제거함
  renderCars() // 삭제 반영해서 다시 그림
}
